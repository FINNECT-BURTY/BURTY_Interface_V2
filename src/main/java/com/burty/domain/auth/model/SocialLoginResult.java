/**
 *
 *
 * <pre>
 * <b>Description  : 인증 도메인 모델 (SocialLoginResult)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.auth.model
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
package com.burty.domain.auth.model;

public record SocialLoginResult(
    String userId,
    String provider,
    String accessToken,
    String refreshToken,
    long accessExpiresInSeconds,
    long refreshExpiresInSeconds,
    boolean newUser,
    boolean profileComplete,
    String frontendOrigin) {}
