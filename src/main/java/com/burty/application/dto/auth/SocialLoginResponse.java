/**
 *
 *
 * <pre>
 * <b>Description  : 인증 응답 DTO (SocialLoginResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.dto.auth
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
package com.burty.application.dto.auth;

import com.burty.domain.auth.model.SocialLoginResult;

public record SocialLoginResponse(
    String userId,
    String provider,
    String accessToken,
    String refreshToken,
    long accessExpiresInSeconds,
    long refreshExpiresInSeconds,
    boolean newUser,
    boolean profileComplete) {
  public static SocialLoginResponse from(SocialLoginResult result) {
    return new SocialLoginResponse(
        result.userId(),
        result.provider(),
        result.accessToken(),
        result.refreshToken(),
        result.accessExpiresInSeconds(),
        result.refreshExpiresInSeconds(),
        result.newUser(),
        result.profileComplete());
  }
}
