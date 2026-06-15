/**
 *
 *
 * <pre>
 * <b>Description  : 보안 포트 인터페이스 (WebAuthnCeremonyPort)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.out.security
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
package com.burty.application.port.out.security;

import com.burty.domain.auth.model.BiometricAuthResult;

public interface WebAuthnCeremonyPort {
  String issueChallenge(String userId, String flowType);

  boolean verifyAndConsumeChallenge(
      String userId, String challengeId, String signedPayload, String flowType);

  BiometricAuthResult registerTrustedDevice(
      String userId,
      String challengeId,
      String signedPayload,
      String deviceFingerprint,
      String platform,
      String biometricType);

  BiometricAuthResult authenticateTrustedDevice(
      String userId, String challengeId, String signedPayload, String deviceToken);

  void saveCredential(String userId, String credentialId);
}
