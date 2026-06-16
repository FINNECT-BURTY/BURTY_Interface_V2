/**
 *
 *
 * <pre>
 * <b>Description  : 인증 유스케이스 포트 (SocialLoginUseCase)</b>
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

import com.burty.domain.auth.model.SocialAuthorizeUrlResult;
import com.burty.domain.auth.model.SocialLoginResult;

public interface SocialLoginUseCase {
  /**
   * @param frontendOrigin 로그인 완료 후 돌려보낼 프론트 origin. null/blank이면 서버 기본 FRONTEND_URL 사용.
   */
  SocialAuthorizeUrlResult createAuthorizeUrl(String provider, String state, String frontendOrigin);

  SocialLoginResult login(
      String provider, String code, String redirectUri, String state, String codeVerifier);
}
