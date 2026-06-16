/**
 *
 *
 * <pre>
 * <b>Description  : 인증 응답 DTO (TokenPairResponse)</b>
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

public record TokenPairResponse(
    String accessToken,
    String refreshToken,
    long accessExpiresInSeconds,
    long refreshExpiresInSeconds) {
  public TokenPairResponse(String accessToken, String refreshToken) {
    this(accessToken, refreshToken, 0L, 0L);
  }
}
