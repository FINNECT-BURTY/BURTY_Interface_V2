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
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
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
    if ("REGISTRATION".equals(flowType)) {
      requireCredentialKey(userId);
    }
    String challengeId = UUID.randomUUID().toString();
    challengeStore.put(challengeId, userId + "|" + flowType, properties.getChallengeTtlSeconds());
    return challengeId;
  }

  @Override
  public boolean verifyAndConsumeChallenge(
      String userId, String challengeId, String signedPayload, String flowType) {
    Verification verification = consumeAndVerify(userId, challengeId, signedPayload, flowType);
    if (verification == null) {
      return false;
    }
    Long userKey = verification.userKey();
    if (userKey != null) {
      if ("REGISTRATION".equals(flowType)) {
        // 기기 정보 없이 등록을 끝내는 경로. 지문을 모르므로 기본 기기에 묶는다.
        // 기기 정보가 있는 등록은 registerTrustedDevice 가 실제 기기에 바로 묶는다.
        credentialStore.upsertFromRegistration(
            userKey,
            verification.result(),
            deviceTrustManager.ensureTrustedDevice(
                userKey, "default-fingerprint-" + userKey, "WEB", null));
      } else {
        credentialStore.updateAfterAuthentication(userKey, verification.result());
      }
    }
    return true;
  }

  /**
   * 챌린지를 소비하고 서명을 검증한다. 통과하지 못하면 {@code null}.
   *
   * <p>자격증명과 기기를 어디에 저장할지는 호출부가 정한다. 예전에는 등록 검증이 기본 기기를 만들고 자격증명을 거기에 묶었고, 이어서 등록 단계가 실제 기기를 또
   * 만들었다. 등록할 때마다 쓰이지 않는 신뢰 기기가 하나씩 남았다.
   */
  private Verification consumeAndVerify(
      String userId, String challengeId, String signedPayload, String flowType) {
    String session = challengeStore.get(challengeId);
    if (session == null) {
      return null;
    }
    // 챌린지는 검증 결과와 무관하게 한 번만 쓸 수 있어야 한다. 성공했을 때만 지우면
    // 실패한 시도가 챌린지를 남겨, TTL 이 다할 때까지 같은 챌린지로 몇 번이든 다시 시도할 수 있다.
    // 선점에 실패했다는 것은 다른 요청이 먼저 소비했다는 뜻이다.
    if (!challengeStore.consume(challengeId)) {
      return null;
    }
    String[] split = session.split("\\|");
    if (split.length != 2) {
      return null;
    }
    if (!split[0].equals(userId) || !split[1].equals(flowType)) {
      return null;
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
    return result.isVerified() ? new Verification(userKey, result) : null;
  }

  private record Verification(Long userKey, WebAuthnAssertionVerifier.VerificationResult result) {}

  @Override
  public BiometricAuthResult registerTrustedDevice(
      String userId,
      String challengeId,
      String signedPayload,
      String deviceFingerprint,
      String platform,
      String biometricType) {
    // 등록할 수 없는 계정이면 챌린지를 쓰기 전에 그 이유로 끝낸다. 검증부터 하면
    // 인증기가 거부한 것과 구분되지 않는 authenticated=false 만 남는다.
    long userKey = requireCredentialKey(userId);
    Verification verification =
        consumeAndVerify(userId, challengeId, signedPayload, "REGISTRATION");
    if (verification == null) {
      return new BiometricAuthResult(userId, null, null, null, false, false);
    }
    // 실제 기기를 만들고 자격증명을 거기에 바로 묶는다.
    WebAuthnDeviceTrustManager.DeviceTokenPair tokenPair =
        deviceTrustManager.ensureTrustedDevice(userKey, deviceFingerprint, platform, null);
    credentialStore.upsertFromRegistration(userKey, verification.result(), tokenPair);
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
   * 자격증명을 저장할 수 있는 계정의 키.
   *
   * <p>등록이 애초에 불가능한 계정이면 챌린지를 내주기 전에 {@link ErrorCode#PASSKEY_UNAVAILABLE} 로 끝낸다. 데모 세션은 사용자 행이 없어
   * 자격증명을 저장할 곳이 없다. 등록을 막는 것 자체는 의도다 — 데모 세션이 LEVEL_3(이체)에 닿으면 안 된다. 바로잡는 것은 그 이유가 보이지 않던 점이다.
   */
  private long requireCredentialKey(String userId) {
    Long userKey = parseUserKey(userId);
    if (userKey == null) {
      throw new BusinessException(ErrorCode.PASSKEY_UNAVAILABLE);
    }
    return userKey;
  }

  /**
   * 사용자 키.
   *
   * <p>자격증명과 기기는 숫자 키로 저장된다. 숫자가 아닌 {@code userId}(데모 세션의 {@code demo-user} 같은)는 여기서 {@code null} 이
   * 된다. 등록은 {@link #requireCredentialKey} 가 이유를 붙여 거부하고, 인증은 등록된 기기가 없으니 실패로 끝난다.
   *
   * <p>예전에는 등록 실패가 조용했다. 화면에는 "인증이 확인되지 않았습니다" 로만 보여서, <b>등록이 애초에 불가능한 것</b>과 인증기가 거부한 것을 구분할 수
   * 없었다. 실제로 이 때문에 origin 설정 문제를 찾다가 엉뚱한 곳을 뒤졌다. 원인이 검증이 아니라 여기일 때는 그렇다고 남긴다.
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
