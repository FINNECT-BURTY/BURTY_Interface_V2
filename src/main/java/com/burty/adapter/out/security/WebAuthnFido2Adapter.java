package com.burty.adapter.out.security;

import com.burty.application.port.out.BiometricAuthPort;
import com.burty.application.port.out.WebAuthnCeremonyPort;
import com.burty.adapter.out.store.ChallengeStore;
import com.burty.config.WebAuthnProperties;
import com.burty.domain.model.BiometricAuthResult;
import com.burty.security.WebAuthnAssertionVerifier;
import com.burty.security.WebAuthnStoredCredential;
import com.burty.domain.entity.BiometricCredentialEntity;
import com.burty.domain.entity.DeviceEntity;
import com.burty.domain.entity.UserEntity;
import com.burty.domain.repository.BiometricCredentialRepository;
import com.burty.domain.repository.DeviceRepository;
import com.burty.domain.repository.UserRepository;
import com.burty.security.JwtTokenProvider;
import com.burty.util.EncryptionUtil;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Component
public class WebAuthnFido2Adapter implements BiometricAuthPort, WebAuthnCeremonyPort {
    private final ChallengeStore challengeStore;
    private final WebAuthnProperties properties;
    private final WebAuthnAssertionVerifier assertionVerifier;
    private final BiometricCredentialRepository biometricCredentialRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final EncryptionUtil encryptionUtil;

    public WebAuthnFido2Adapter(WebAuthnProperties properties, ChallengeStore challengeStore, WebAuthnAssertionVerifier assertionVerifier,
                                BiometricCredentialRepository biometricCredentialRepository, UserRepository userRepository,
                                DeviceRepository deviceRepository, JwtTokenProvider jwtTokenProvider,
                                EncryptionUtil encryptionUtil) {
        this.properties = properties;
        this.challengeStore = challengeStore;
        this.assertionVerifier = assertionVerifier;
        this.biometricCredentialRepository = biometricCredentialRepository;
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.encryptionUtil = encryptionUtil;
    }

    @Override
    public String issueChallenge(String userId, String flowType) {
        String challengeId = UUID.randomUUID().toString();
        challengeStore.put(challengeId, userId + "|" + flowType, properties.getChallengeTtlSeconds());
        return challengeId;
    }

    @Override
    public boolean verifyAndConsumeChallenge(String userId, String challengeId, String signedPayload, String flowType) {
        String session = challengeStore.get(challengeId);
        if (session == null) return false;
        String[] split = session.split("\\|");
        if (split.length != 2) return false;
        if (!split[0].equals(userId) || !split[1].equals(flowType)) return false;
        Long userKey = parseUserKey(userId);
        long currentSignCount = findSignCount(userKey);
        WebAuthnStoredCredential stored = toStoredCredential(
                "REGISTRATION".equals(flowType) ? null : biometricCredentialRepository.findFirstByUser_UserIdAndRevokedAtIsNull(userKey).orElse(null)
        );
        WebAuthnAssertionVerifier.VerificationResult result = "REGISTRATION".equals(flowType)
                ? assertionVerifier.verifyRegistration(signedPayload, challengeId, properties.getOrigin(), properties.getRpId(), currentSignCount)
                : assertionVerifier.verifyAuthentication(signedPayload, challengeId, properties.getOrigin(), properties.getRpId(), currentSignCount, stored);
        if (!result.isVerified()) return false;
        if (userKey != null) {
            if ("REGISTRATION".equals(flowType)) {
                upsertCredential(userKey, result);
            } else {
                updateCredentialAfterAuthentication(userKey, result);
            }
        }
        challengeStore.remove(challengeId);
        return true;
    }

    @Override
    public BiometricAuthResult registerTrustedDevice(String userId, String challengeId, String signedPayload,
                                                     String deviceFingerprint, String platform, String biometricType) {
        boolean verified = verifyAndConsumeChallenge(userId, challengeId, signedPayload, "REGISTRATION");
        if (!verified) {
            return new BiometricAuthResult(userId, null, null, null, false, false);
        }
        Long userKey = parseUserKey(userId);
        if (userKey == null) {
            return new BiometricAuthResult(userId, null, null, null, false, false);
        }
        DeviceTokenPair tokenPair = ensureTrustedDevice(userKey, deviceFingerprint, platform, null);
        BiometricCredentialEntity credential = biometricCredentialRepository
                .findFirstByUser_UserIdAndRevokedAtIsNull(userKey)
                .orElse(null);
        if (credential != null) {
            credential.setDevice(tokenPair.device());
            credential.setCredentialType(parseCredentialType(biometricType));
            credential.setLastUsedAt(LocalDateTime.now());
            biometricCredentialRepository.save(credential);
        }
        return new BiometricAuthResult(
                userId,
                tokenPair.device().getDeviceId().toString(),
                tokenPair.plainToken(),
                jwtTokenProvider.generateToken(userId),
                true,
                true
        );
    }

    @Override
    public BiometricAuthResult authenticateTrustedDevice(String userId, String challengeId, String signedPayload, String deviceToken) {
        Long userKey = parseUserKey(userId);
        DeviceEntity device = findDeviceByToken(userKey, deviceToken);
        if (userKey == null || device == null || !Boolean.TRUE.equals(device.getIsTrusted())) {
            return new BiometricAuthResult(userId, null, null, null, false, false);
        }
        boolean verified = verifyAndConsumeChallenge(userId, challengeId, signedPayload, "AUTHENTICATION");
        if (!verified) {
            return new BiometricAuthResult(userId, device.getDeviceId().toString(), null, null, false, true);
        }
        device.setLastSeenAt(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());
        deviceRepository.save(device);
        return new BiometricAuthResult(
                userId,
                device.getDeviceId().toString(),
                null,
                jwtTokenProvider.generateToken(userId),
                true,
                true
        );
    }

    @Override
    public void saveCredential(String userId, String credentialId) {
        Long userKey = parseUserKey(userId);
        if (userKey == null) return;
        UserEntity user = userRepository.findById(userKey).orElse(null);
        if (user == null) return;
        BiometricCredentialEntity entity = biometricCredentialRepository
                .findFirstByUser_UserIdAndRevokedAtIsNull(userKey)
                .orElseGet(BiometricCredentialEntity::new);
        if (entity.getCredentialId() == null) entity.setCredentialId(UUID.randomUUID());
        entity.setUser(user);
        entity.setCredentialType(BiometricCredentialEntity.CredentialType.FINGERPRINT);
        entity.setPublicKey(("public:" + credentialId).getBytes(StandardCharsets.UTF_8));
        entity.setCredentialIdRaw(credentialId.getBytes(StandardCharsets.UTF_8));
        entity.setRegisteredAt(java.time.LocalDateTime.now());
        entity.setSignCount(entity.getSignCount() == null ? 0L : entity.getSignCount());
        entity.setDevice(defaultDevice(userKey));
        biometricCredentialRepository.save(entity);
    }

    @Override
    public boolean verifyAssertion(String userId, String assertionToken) {
        Long userKey = parseUserKey(userId);
        if (userKey == null) return false;
        BiometricCredentialEntity credential = biometricCredentialRepository
                .findFirstByUser_UserIdAndRevokedAtIsNull(userKey).orElse(null);
        if (credential == null) return false;
        if (assertionToken == null || !assertionToken.startsWith("webauthn:")) return false;
        String signature = assertionToken.substring("webauthn:".length());
        String expected = sign(userId + ":" + new String(credential.getCredentialIdRaw(), StandardCharsets.UTF_8));
        return expected.equals(signature);
    }

    private long findSignCount(Long userKey) {
        if (userKey == null) return 0L;
        return biometricCredentialRepository.findFirstByUser_UserIdAndRevokedAtIsNull(userKey)
                .map(BiometricCredentialEntity::getSignCount)
                .orElse(0L);
    }

    private void updateCredentialAfterAuthentication(Long userKey, WebAuthnAssertionVerifier.VerificationResult result) {
        BiometricCredentialEntity entity = biometricCredentialRepository
                .findFirstByUser_UserIdAndRevokedAtIsNull(userKey).orElse(null);
        if (entity == null) return;
        entity.setSignCount(result.getNextSignCount());
        entity.setLastUsedAt(LocalDateTime.now());
        biometricCredentialRepository.save(entity);
    }

    private WebAuthnStoredCredential toStoredCredential(BiometricCredentialEntity cred) {
        if (cred == null) {
            return new WebAuthnStoredCredential(new byte[0], new byte[0], 0L);
        }
        long sc = cred.getSignCount() == null ? 0L : cred.getSignCount();
        return new WebAuthnStoredCredential(
                maybeDecodeBase64Url(cred.getCredentialIdRaw()),
                maybeDecodeBase64Url(cred.getPublicKey()),
                sc
        );
    }

    private static byte[] maybeDecodeBase64Url(byte[] stored) {
        if (stored == null || stored.length == 0) {
            return new byte[0];
        }
        try {
            String s = new String(stored, StandardCharsets.UTF_8);
            return Base64.getUrlDecoder().decode(s);
        } catch (Exception e) {
            return stored.clone();
        }
    }

    private void upsertCredential(Long userKey, WebAuthnAssertionVerifier.VerificationResult result) {
        UserEntity user = userRepository.findById(userKey).orElse(null);
        if (user == null) return;
        BiometricCredentialEntity entity = biometricCredentialRepository
                .findFirstByUser_UserIdAndRevokedAtIsNull(userKey)
                .orElseGet(BiometricCredentialEntity::new);
        if (entity.getCredentialId() == null) entity.setCredentialId(UUID.randomUUID());
        entity.setUser(user);
        entity.setDevice(defaultDevice(userKey));
        entity.setCredentialType(BiometricCredentialEntity.CredentialType.FINGERPRINT);
        entity.setPublicKey(result.getPublicKey().getBytes(StandardCharsets.UTF_8));
        entity.setCredentialIdRaw(result.getCredentialIdRaw().getBytes(StandardCharsets.UTF_8));
        entity.setSignCount(result.getNextSignCount());
        if (entity.getRegisteredAt() == null) entity.setRegisteredAt(java.time.LocalDateTime.now());
        entity.setLastUsedAt(java.time.LocalDateTime.now());
        biometricCredentialRepository.save(entity);
    }

    private DeviceEntity defaultDevice(Long userKey) {
        return ensureTrustedDevice(userKey, "default-fingerprint-" + userKey, "WEB", null).device();
    }

    private DeviceTokenPair ensureTrustedDevice(Long userKey, String deviceFingerprint, String platform, String existingToken) {
        UserEntity user = userRepository.findById(userKey).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("user not found");
        }
        String fingerprint = normalizeFingerprint(deviceFingerprint, userKey);
        DeviceEntity device = deviceRepository
                .findByUser_UserIdAndDeviceFingerprintAndRevokedAtIsNull(userKey, fingerprint)
                .orElseGet(DeviceEntity::new);
        String plainToken = existingToken;
        if (device.getDeviceId() == null) {
            device.setDeviceId(UUID.randomUUID());
            plainToken = blank(plainToken) ? issueDeviceToken(userKey, fingerprint) : plainToken;
            device.setDeviceTokenHash(sha256(plainToken));
            device.setDeviceTokenEncrypted(encryptionUtil.encrypt(plainToken));
            device.setCreatedAt(LocalDateTime.now());
        } else if (blank(plainToken)) {
            plainToken = encryptionUtil.decrypt(device.getDeviceTokenEncrypted());
        }
        device.setUser(user);
        device.setDeviceFingerprint(fingerprint);
        device.setPlatform(parsePlatform(platform));
        device.setIsTrusted(true);
        device.setLastSeenAt(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());
        return new DeviceTokenPair(deviceRepository.save(device), plainToken);
    }

    private DeviceEntity findDeviceByToken(Long userKey, String deviceToken) {
        if (userKey == null || blank(deviceToken)) return null;
        return deviceRepository.findByDeviceTokenHashAndRevokedAtIsNull(sha256(deviceToken))
                .filter(device -> device.getUser() != null && userKey.equals(device.getUser().getUserId()))
                .orElse(null);
    }

    private String issueDeviceToken(Long userKey, String fingerprint) {
        return "bdt_" + Base64.getUrlEncoder().withoutPadding()
                .encodeToString((userKey + ":" + fingerprint + ":" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));
    }

    private String normalizeFingerprint(String deviceFingerprint, Long userKey) {
        if (!blank(deviceFingerprint)) return sha256(deviceFingerprint);
        return sha256("default-fingerprint-" + userKey);
    }

    private DeviceEntity.Platform parsePlatform(String platform) {
        if (blank(platform)) return DeviceEntity.Platform.WEB;
        try {
            return DeviceEntity.Platform.valueOf(platform.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DeviceEntity.Platform.WEB;
        }
    }

    private BiometricCredentialEntity.CredentialType parseCredentialType(String biometricType) {
        if (blank(biometricType)) return BiometricCredentialEntity.CredentialType.FINGERPRINT;
        String normalized = biometricType.trim().toUpperCase().replace("-", "_");
        if ("FACE".equals(normalized) || "FACEID".equals(normalized)) normalized = "FACE_ID";
        try {
            return BiometricCredentialEntity.CredentialType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return BiometricCredentialEntity.CredentialType.FINGERPRINT;
        }
    }

    private Long parseUserKey(String value) {
        try { return Long.parseLong(value); } catch (Exception e) { return null; }
    }

    private String sign(String raw) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(properties.getServerSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac.doFinal(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "";
        }
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record DeviceTokenPair(DeviceEntity device, String plainToken) {}

}
