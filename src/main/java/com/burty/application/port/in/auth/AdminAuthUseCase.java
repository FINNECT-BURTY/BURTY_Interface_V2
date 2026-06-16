/**
 *
 *
 * <pre>
 * <b>Description  : 인증 유스케이스 포트 (AdminAuthUseCase)</b>
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

public interface AdminAuthUseCase {

  String login(String username, String password);

  void register(String setupKey, String username, String password, String role);
}
