/**
 *
 *
 * <pre>
 * <b>Description  : 보안 외부 연동 어댑터 (WebAuthnFido2Adapter)</b>
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

import com.burty.adapter.out.store.ChallengeStore;
import com.burty.application.port.out.security.BiometricAuthPort;
import com.burty.application.port.out.security.WebAuthnCeremonyPort;
import com.burty.application.service.support.AuditLogger;
import com.burty.config.WebAuthnProperties;
import com.burty.domain.auth.model.BiometricAuthResult;
import com.burty.domain.user.entity.DeviceEntity;
import com.burty.security.JwtTokenProvider;
import com.burty.security.WebAuthnAssertionVerifier;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebAuthnFido2Adapter implements BiometricAuthPort, WebAuthnCeremonyPort {

  private final ChallengeStore challengeStore;
  private final WebAuthnProperties properties;
  private final WebAuthnAssertionVerifier assertionVerifier;
  private final WebAuthnCredentialStore credentialStore;
  private final WebAuthnDeviceTrustManager deviceTrustManager;
  private final JwtTokenProvider jwtTokenProvider;
  private final AuditLogger auditLogger;

  public WebAuthnFido2Adapter(
      WebAuthnProperties properties,
      ChallengeStore challengeStore,
      WebAuthnAssertionVerifier assertionVerifier,
      WebAuthnCredentialStore credentialStore,
      WebAuthnDeviceTrustManager deviceTrustManager,
      JwtTokenProvider jwtTokenProvider,
      AuditLogger auditLogger) {
    this.properties = properties;
    this.challengeStore = challengeStore;
    this.assertionVerifier = assertionVerifier;
    this.credentialStore = credentialStore;
    this.deviceTrustManager = deviceTrustManager;
    this.jwtTokenProvider = jwtTokenProvider;
    this.auditLogger = auditLogger;
  }

  @Override
  public String issueChallenge(String userId, String flowType) {
    String challengeId = UUID.randomUUID().toString();
    challengeStore.put(challengeId, userId + "|" + flowType, properties.getChallengeTtlSeconds());
    return challengeId;
  }

  @Override
  public boolean verifyAndConsumeChallenge(
      String userId, String challengeId, String signedPayload, String flowType) {
    String session = challengeStore.get(challengeId);
    if (session == null) {
      return false;
    }
    // 챌린지는 검증 결과와 무관하게 한 번만 쓸 수 있어야 한다. 성공했을 때만 지우면
    // 실패한 시도가 챌린지를 남겨, TTL 이 다할 때까지 같은 챌린지로 몇 번이든 다시 시도할 수 있다.
    // 선점에 실패했다는 것은 다른 요청이 먼저 소비했다는 뜻이다.
    if (!challengeStore.consume(challengeId)) {
      return false;
    }
    String[] split = session.split("\\|");
    if (split.length != 2) {
      return false;
    }
    if (!split[0].equals(userId) || !split[1].equals(flowType)) {
      return false;
    }
    Long userKey = parseUserKey(userId);
    long currentSignCount = credentialStore.findSignCount(userKey);
    WebAuthnAssertionVerifier.VerificationResult result =
        "REGISTRATION".equals(flowType)
            ? assertionVerifier.verifyRegistration(
                signedPayload,
                challengeId,
                properties.getOrigin(),
                properties.getRpId(),
                currentSignCount)
            : assertionVerifier.verifyAuthentication(
                signedPayload,
                challengeId,
                properties.getOrigin(),
                properties.getRpId(),
                currentSignCount,
                credentialStore.findStoredCredential(userKey));
    if (!result.isVerified()) {
      return false;
    }
    if (userKey != null) {
      if ("REGISTRATION".equals(flowType)) {
        credentialStore.upsertFromRegistration(
            userKey,
            result,
            deviceTrustManager.ensureTrustedDevice(
                userKey, "default-fingerprint-" + userKey, "WEB", null));
      } else {
        credentialStore.updateAfterAuthentication(userKey, result);
      }
    }
    return true;
  }

  @Override
  public BiometricAuthResult registerTrustedDevice(
      String userId,
      String challengeId,
      String signedPayload,
      String deviceFingerprint,
      String platform,
      String biometricType) {
    boolean verified =
        verifyAndConsumeChallenge(userId, challengeId, signedPayload, "REGISTRATION");
    if (!verified) {
      return new BiometricAuthResult(userId, null, null, null, false, false);
    }
    Long userKey = parseUserKey(userId);
    if (userKey == null) {
      return new BiometricAuthResult(userId, null, null, null, false, false);
    }
    WebAuthnDeviceTrustManager.DeviceTokenPair tokenPair =
        deviceTrustManager.ensureTrustedDevice(userKey, deviceFingerprint, platform, null);
    credentialStore.bindCredentialToDevice(userKey, tokenPair, biometricType);
    auditLogger.logSuccess(
        userId, "WEBAUTHN_REGISTER", tokenPair.device().getDeviceId().toString(), platform);
    return new BiometricAuthResult(
        userId,
        tokenPair.device().getDeviceId().toString(),
        tokenPair.plainToken(),
        jwtTokenProvider.generateToken(userId),
        true,
        true);
  }

  @Override
  public BiometricAuthResult authenticateTrustedDevice(
      String userId, String challengeId, String signedPayload, String deviceToken) {
    Long userKey = parseUserKey(userId);
    DeviceEntity device = deviceTrustManager.findDeviceByToken(userKey, deviceToken);
    if (userKey == null || device == null || !Boolean.TRUE.equals(device.getIsTrusted())) {
      return new BiometricAuthResult(userId, null, null, null, false, false);
    }
    boolean verified =
        verifyAndConsumeChallenge(userId, challengeId, signedPayload, "AUTHENTICATION");
    if (!verified) {
      return new BiometricAuthResult(
          userId, device.getDeviceId().toString(), null, null, false, true);
    }
    deviceTrustManager.touchDevice(device);
    auditLogger.logSuccess(userId, "WEBAUTHN_AUTHENTICATE", device.getDeviceId().toString(), null);
    return new BiometricAuthResult(
        userId,
        device.getDeviceId().toString(),
        null,
        jwtTokenProvider.generateToken(userId),
        true,
        true);
  }

  @Override
  public void saveCredential(String userId, String credentialId) {
    credentialStore.saveCredential(userId, credentialId, deviceTrustManager);
  }

  @Override
  public boolean verifyAssertion(String userId, String assertionToken) {
    return credentialStore.verifyAssertion(userId, assertionToken, properties.getServerSecret());
  }

  /**
   * 사용자 키.
   *
   * <p>자격증명과 기기는 숫자 키로 저장된다. 숫자가 아닌 {@code userId}(데모 세션의 {@code demo-user} 같은)는 여기서 {@code null} 이
   * 되고, 호출부는 인증 실패로 끝낸다.
   *
   * <p>예전에는 그 실패가 조용했다. 화면에는 "인증이 확인되지 않았습니다" 로만 보여서, <b>등록이 애초에 불가능한 것</b>과 인증기가 거부한 것을 구분할 수 없었다.
   * 실제로 이 때문에 origin 설정 문제를 찾다가 엉뚱한 곳을 뒤졌다. 원인이 검증이 아니라 여기일 때는 그렇다고 남긴다.
   */
  private Long parseUserKey(String value) {
    try {
      return Long.parseLong(value);
    } catch (Exception e) {
      log.warn("숫자가 아닌 userId 라 자격증명을 찾을 수 없다 — {}. 이 계정은 패스키를 등록할 수 없다.", value);
      return null;
    }
  }
}
