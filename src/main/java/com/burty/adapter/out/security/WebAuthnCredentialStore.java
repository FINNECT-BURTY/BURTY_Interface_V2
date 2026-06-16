/**
 *
 *
 * <pre>
 * <b>Description  : 보안 (WebAuthnCredentialStore)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.out.security
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.adapter.out.security;

import com.burty.domain.auth.entity.BiometricCredentialEntity;
import com.burty.domain.auth.repository.BiometricCredentialRepository;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.UserRepository;
import com.burty.security.WebAuthnAssertionVerifier;
import com.burty.security.WebAuthnStoredCredential;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class WebAuthnCredentialStore {

  private final BiometricCredentialRepository biometricCredentialRepository;
  private final UserRepository userRepository;

  public WebAuthnCredentialStore(
      BiometricCredentialRepository biometricCredentialRepository, UserRepository userRepository) {
    this.biometricCredentialRepository = biometricCredentialRepository;
    this.userRepository = userRepository;
  }

  public long findSignCount(Long userKey) {
    if (userKey == null) {
      return 0L;
    }
    return biometricCredentialRepository
        .findFirstByUser_UserIdAndRevokedAtIsNull(userKey)
        .map(BiometricCredentialEntity::getSignCount)
        .orElse(0L);
  }

  public WebAuthnStoredCredential findStoredCredential(Long userKey) {
    if (userKey == null) {
      return new WebAuthnStoredCredential(new byte[0], new byte[0], 0L);
    }
    return toStoredCredential(
        biometricCredentialRepository
            .findFirstByUser_UserIdAndRevokedAtIsNull(userKey)
            .orElse(null));
  }

  public void updateAfterAuthentication(
      Long userKey, WebAuthnAssertionVerifier.VerificationResult result) {
    BiometricCredentialEntity entity =
        biometricCredentialRepository
            .findFirstByUser_UserIdAndRevokedAtIsNull(userKey)
            .orElse(null);
    if (entity == null) {
      return;
    }
    entity.setSignCount(result.getNextSignCount());
    entity.setLastUsedAt(LocalDateTime.now());
    biometricCredentialRepository.save(entity);
  }

  public void upsertFromRegistration(
      Long userKey,
      WebAuthnAssertionVerifier.VerificationResult result,
      WebAuthnDeviceTrustManager.DeviceTokenPair devicePair) {
    UserEntity user = userRepository.findById(userKey).orElse(null);
    if (user == null) {
      return;
    }
    BiometricCredentialEntity entity =
        biometricCredentialRepository
            .findFirstByUser_UserIdAndRevokedAtIsNull(userKey)
            .orElseGet(BiometricCredentialEntity::new);
    entity.setUser(user);
    entity.setDevice(devicePair.device());
    entity.setCredentialType(BiometricCredentialEntity.CredentialType.FINGERPRINT);
    entity.setPublicKey(result.getPublicKey().getBytes(StandardCharsets.UTF_8));
    entity.setCredentialIdRaw(result.getCredentialIdRaw().getBytes(StandardCharsets.UTF_8));
    entity.setSignCount(result.getNextSignCount());
    if (entity.getRegisteredAt() == null) {
      entity.setRegisteredAt(LocalDateTime.now());
    }
    entity.setLastUsedAt(LocalDateTime.now());
    biometricCredentialRepository.save(entity);
  }

  public void saveCredential(
      String userId, String credentialId, WebAuthnDeviceTrustManager deviceTrust) {
    Long userKey = parseUserKey(userId);
    if (userKey == null) {
      return;
    }
    UserEntity user = userRepository.findById(userKey).orElse(null);
    if (user == null) {
      return;
    }
    BiometricCredentialEntity entity =
        biometricCredentialRepository
            .findFirstByUser_UserIdAndRevokedAtIsNull(userKey)
            .orElseGet(BiometricCredentialEntity::new);
    entity.setUser(user);
    entity.setCredentialType(BiometricCredentialEntity.CredentialType.FINGERPRINT);
    entity.setPublicKey(("public:" + credentialId).getBytes(StandardCharsets.UTF_8));
    entity.setCredentialIdRaw(credentialId.getBytes(StandardCharsets.UTF_8));
    entity.setRegisteredAt(LocalDateTime.now());
    entity.setSignCount(entity.getSignCount() == null ? 0L : entity.getSignCount());
    entity.setDevice(deviceTrust.defaultDevice(userKey));
    biometricCredentialRepository.save(entity);
  }

  public boolean verifyAssertion(String userId, String assertionToken, String serverSecret) {
    Long userKey = parseUserKey(userId);
    if (userKey == null) {
      return false;
    }
    BiometricCredentialEntity credential =
        biometricCredentialRepository
            .findFirstByUser_UserIdAndRevokedAtIsNull(userKey)
            .orElse(null);
    if (credential == null) {
      return false;
    }
    if (assertionToken == null || !assertionToken.startsWith("webauthn:")) {
      return false;
    }
    String signature = assertionToken.substring("webauthn:".length());
    String expected =
        WebAuthnSignature.sign(
            userId + ":" + new String(credential.getCredentialIdRaw(), StandardCharsets.UTF_8),
            serverSecret);
    return expected.equals(signature);
  }

  public BiometricCredentialEntity findActiveCredential(Long userKey) {
    return biometricCredentialRepository
        .findFirstByUser_UserIdAndRevokedAtIsNull(userKey)
        .orElse(null);
  }

  public void bindCredentialToDevice(
      Long userKey, WebAuthnDeviceTrustManager.DeviceTokenPair devicePair, String biometricType) {
    BiometricCredentialEntity credential =
        biometricCredentialRepository
            .findFirstByUser_UserIdAndRevokedAtIsNull(userKey)
            .orElse(null);
    if (credential == null) {
      return;
    }
    credential.setDevice(devicePair.device());
    credential.setCredentialType(parseCredentialType(biometricType));
    credential.setLastUsedAt(LocalDateTime.now());
    biometricCredentialRepository.save(credential);
  }

  private static WebAuthnStoredCredential toStoredCredential(BiometricCredentialEntity cred) {
    if (cred == null) {
      return new WebAuthnStoredCredential(new byte[0], new byte[0], 0L);
    }
    long signCount = cred.getSignCount() == null ? 0L : cred.getSignCount();
    return new WebAuthnStoredCredential(
        maybeDecodeBase64Url(cred.getCredentialIdRaw()),
        maybeDecodeBase64Url(cred.getPublicKey()),
        signCount);
  }

  private static byte[] maybeDecodeBase64Url(byte[] stored) {
    if (stored == null || stored.length == 0) {
      return new byte[0];
    }
    try {
      String value = new String(stored, StandardCharsets.UTF_8);
      return Base64.getUrlDecoder().decode(value);
    } catch (Exception e) {
      return stored.clone();
    }
  }

  private static BiometricCredentialEntity.CredentialType parseCredentialType(
      String biometricType) {
    if (biometricType == null || biometricType.isBlank()) {
      return BiometricCredentialEntity.CredentialType.FINGERPRINT;
    }
    String normalized = biometricType.trim().toUpperCase().replace("-", "_");
    if ("FACE".equals(normalized) || "FACEID".equals(normalized)) {
      normalized = "FACE_ID";
    }
    try {
      return BiometricCredentialEntity.CredentialType.valueOf(normalized);
    } catch (IllegalArgumentException e) {
      return BiometricCredentialEntity.CredentialType.FINGERPRINT;
    }
  }

  private static Long parseUserKey(String value) {
    try {
      return Long.parseLong(value);
    } catch (Exception e) {
      return null;
    }
  }
}
