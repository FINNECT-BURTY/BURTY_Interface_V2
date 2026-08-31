/**
 *
 *
 * <pre>
 * <b>Description  : 인증 유스케이스 포트 (SessionManagementUseCase)</b>
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

import com.burty.application.dto.auth.SessionResponse;
import com.burty.application.dto.auth.TokenPairResponse;
import java.util.List;

public interface SessionManagementUseCase {

  TokenPairResponse createSession(String userId, String deviceId);

  TokenPairResponse refreshSession(String refreshToken);

  List<SessionResponse> listActiveSessions(String userId);

  /**
   * 세션 하나를 종료한다.
   *
   * <p>{@code userId} 를 함께 받는다. 세션 ID 만으로 종료하면 남의 세션도 끊을 수 있고, ID 가 auto_increment 라 열거도 쉽다.
   */
  void revokeSession(String userId, String sessionId);

  void revokeAllSessions(String userId);
}
