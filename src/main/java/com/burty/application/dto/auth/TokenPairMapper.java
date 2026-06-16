/**
 *
 *
 * <pre>
 * <b>Description  : 인증 응답 매퍼 (TokenPairMapper)</b>
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

import com.burty.security.RefreshTokenService;

public final class TokenPairMapper {

  private TokenPairMapper() {}

  public static TokenPairResponse toResponse(RefreshTokenService.TokenPair pair) {
    return new TokenPairResponse(
        pair.accessToken(),
        pair.refreshToken(),
        pair.accessExpiresInSeconds(),
        pair.refreshExpiresInSeconds());
  }
}
