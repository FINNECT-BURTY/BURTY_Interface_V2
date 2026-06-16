/**
 *
 *
 * <pre>
 * <b>Description  : 인증 유스케이스 포트 (WebAuthnUseCase)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.in.auth
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
package com.burty.application.port.in.auth;

import com.burty.domain.auth.model.BiometricAuthResult;

public interface WebAuthnUseCase {
  String beginRegistration(String userId);

  boolean finishRegistration(String userId, String challengeId, String attestation);

  BiometricAuthResult finishRegistration(
      String userId,
      String challengeId,
      String attestation,
      String deviceFingerprint,
      String platform,
      String biometricType);

  String beginAuthentication(String userId);

  boolean finishAuthentication(String userId, String challengeId, String assertion);

  BiometricAuthResult finishAuthentication(
      String userId, String challengeId, String assertion, String deviceToken);
}
