package com.nuri.adapter.out.security;

import com.nuri.application.port.out.BiometricAuthPort;
import com.nuri.application.port.out.WebAuthnCeremonyPort;
import com.nuri.adapter.out.store.ChallengeStore;
import com.nuri.config.WebAuthnProperties;
import com.nuri.security.WebAuthnAssertionVerifier;
import com.nuri.security.WebAuthnStoredCredential;
import com.nuri.domain.entity.BiometricCredentialEntity;
import com.nuri.domain.entity.DeviceEntity;
import com.nuri.domain.entity.UserEntity;
import com.nuri.domain.repository.BiometricCredentialRepository;
import com.nuri.domain.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
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

    public WebAuthnFido2Adapter(WebAuthnProperties properties, ChallengeStore challengeStore, WebAuthnAssertionVerifier assertionVerifier,
                                BiometricCredentialRepository biometricCredentialRepository, UserRepository userRepository) {
        this.properties = properties;
        this.challengeStore = challengeStore;
        this.assertionVerifier = assertionVerifier;
        this.biometricCredentialRepository = biometricCredentialRepository;
        this.userRepository = userRepository;
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
        UUID userUuid = parseUuid(userId);
        long currentSignCount = findSignCount(userUuid);
        WebAuthnStoredCredential stored = toStoredCredential(
                "REGISTRATION".equals(flowType) ? null : biometricCredentialRepository.findFirstByUser_UserIdAndRevokedAtIsNull(userUuid).orElse(null)
        );
        WebAuthnAssertionVerifier.VerificationResult result = "REGISTRATION".equals(flowType)
                ? assertionVerifier.verifyRegistration(signedPayload, challengeId, properties.getOrigin(), properties.getRpId(), currentSignCount)
                : assertionVerifier.verifyAuthentication(signedPayload, challengeId, properties.getOrigin(), properties.getRpId(), currentSignCount, stored);
        if (!result.isVerified()) return false;
        if (userUuid != null) {
            if ("REGISTRATION".equals(flowType)) {
                upsertCredential(userUuid, result);
            } else {
                updateCredentialAfterAuthentication(userUuid, result);
            }
        }
        challengeStore.remove(challengeId);
        return true;
    }

    @Override
    public void saveCredential(String userId, String credentialId) {
        UUID userUuid = parseUuid(userId);
        if (userUuid == null) return;
        UserEntity user = userRepository.findById(userUuid).orElse(null);
        if (user == null) return;
        BiometricCredentialEntity entity = biometricCredentialRepository
                .findFirstByUser_UserIdAndRevokedAtIsNull(userUuid)
                .orElseGet(BiometricCredentialEntity::new);
        if (entity.getCredentialId() == null) entity.setCredentialId(UUID.randomUUID());
        entity.setUser(user);
        entity.setCredentialType(BiometricCredentialEntity.CredentialType.FINGERPRINT);
        entity.setPublicKey(("public:" + credentialId).getBytes(StandardCharsets.UTF_8));
        entity.setCredentialIdRaw(credentialId.getBytes(StandardCharsets.UTF_8));
        entity.setRegisteredAt(java.time.LocalDateTime.now());
        entity.setSignCount(entity.getSignCount() == null ? 0L : entity.getSignCount());
        entity.setDevice(defaultDevice(userUuid));
        biometricCredentialRepository.save(entity);
    }

    @Override
    public boolean verifyAssertion(String userId, String assertionToken) {
        UUID userUuid = parseUuid(userId);
        if (userUuid == null) return false;
        BiometricCredentialEntity credential = biometricCredentialRepository
                .findFirstByUser_UserIdAndRevokedAtIsNull(userUuid).orElse(null);
        if (credential == null) return false;
        if (assertionToken == null || !assertionToken.startsWith("webauthn:")) return false;
        String signature = assertionToken.substring("webauthn:".length());
        String expected = sign(userId + ":" + new String(credential.getCredentialIdRaw(), StandardCharsets.UTF_8));
        return expected.equals(signature);
    }

    private long findSignCount(UUID userUuid) {
        if (userUuid == null) return 0L;
        return biometricCredentialRepository.findFirstByUser_UserIdAndRevokedAtIsNull(userUuid)
                .map(BiometricCredentialEntity::getSignCount)
                .orElse(0L);
    }

    private void updateCredentialAfterAuthentication(UUID userUuid, WebAuthnAssertionVerifier.VerificationResult result) {
        BiometricCredentialEntity entity = biometricCredentialRepository
                .findFirstByUser_UserIdAndRevokedAtIsNull(userUuid).orElse(null);
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

    private void upsertCredential(UUID userUuid, WebAuthnAssertionVerifier.VerificationResult result) {
        UserEntity user = userRepository.findById(userUuid).orElse(null);
        if (user == null) return;
        BiometricCredentialEntity entity = biometricCredentialRepository
                .findFirstByUser_UserIdAndRevokedAtIsNull(userUuid)
                .orElseGet(BiometricCredentialEntity::new);
        if (entity.getCredentialId() == null) entity.setCredentialId(UUID.randomUUID());
        entity.setUser(user);
        entity.setDevice(defaultDevice(userUuid));
        entity.setCredentialType(BiometricCredentialEntity.CredentialType.FINGERPRINT);
        entity.setPublicKey(result.getPublicKey().getBytes(StandardCharsets.UTF_8));
        entity.setCredentialIdRaw(result.getCredentialIdRaw().getBytes(StandardCharsets.UTF_8));
        entity.setSignCount(result.getNextSignCount());
        if (entity.getRegisteredAt() == null) entity.setRegisteredAt(java.time.LocalDateTime.now());
        entity.setLastUsedAt(java.time.LocalDateTime.now());
        biometricCredentialRepository.save(entity);
    }

    private DeviceEntity defaultDevice(UUID userUuid) {
        DeviceEntity d = new DeviceEntity();
        d.setDeviceId(UUID.randomUUID());
        d.setUser(userRepository.findById(userUuid).orElse(null));
        d.setDeviceFingerprint("default-fingerprint-" + userUuid);
        d.setPlatform(DeviceEntity.Platform.WEB);
        d.setCreatedAt(java.time.LocalDateTime.now());
        d.setUpdatedAt(java.time.LocalDateTime.now());
        return d;
    }

    private UUID parseUuid(String value) {
        try { return UUID.fromString(value); } catch (Exception e) { return null; }
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

}
