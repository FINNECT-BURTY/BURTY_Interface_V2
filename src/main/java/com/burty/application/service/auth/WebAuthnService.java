/**
 *
 *
 * <pre>
 * <b>Description  : 인증 애플리케이션 서비스 (WebAuthnService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.auth
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
package com.burty.application.service.auth;

import com.burty.application.port.in.auth.WebAuthnUseCase;
import com.burty.application.port.out.security.WebAuthnCeremonyPort;
import com.burty.domain.auth.model.BiometricAuthResult;
import org.springframework.stereotype.Service;

@Service
public class WebAuthnService implements WebAuthnUseCase {
  private final WebAuthnCeremonyPort webAuthnCeremonyPort;

  public WebAuthnService(WebAuthnCeremonyPort webAuthnCeremonyPort) {
    this.webAuthnCeremonyPort = webAuthnCeremonyPort;
  }

  @Override
  public String beginRegistration(String userId) {
    return webAuthnCeremonyPort.issueChallenge(userId, "REGISTRATION");
  }

  @Override
  public boolean finishRegistration(String userId, String challengeId, String attestation) {
    boolean verified =
        webAuthnCeremonyPort.verifyAndConsumeChallenge(
            userId, challengeId, attestation, "REGISTRATION");
    if (verified) {
      webAuthnCeremonyPort.saveCredential(userId, "cred-" + userId);
    }
    return verified;
  }

  @Override
  public BiometricAuthResult finishRegistration(
      String userId,
      String challengeId,
      String attestation,
      String deviceFingerprint,
      String platform,
      String biometricType) {
    return webAuthnCeremonyPort.registerTrustedDevice(
        userId, challengeId, attestation, deviceFingerprint, platform, biometricType);
  }

  @Override
  public String beginAuthentication(String userId) {
    return webAuthnCeremonyPort.issueChallenge(userId, "AUTHENTICATION");
  }

  @Override
  public boolean finishAuthentication(String userId, String challengeId, String assertion) {
    return webAuthnCeremonyPort.verifyAndConsumeChallenge(
        userId, challengeId, assertion, "AUTHENTICATION");
  }

  @Override
  public BiometricAuthResult finishAuthentication(
      String userId, String challengeId, String assertion, String deviceToken) {
    return webAuthnCeremonyPort.authenticateTrustedDevice(
        userId, challengeId, assertion, deviceToken);
  }
}
