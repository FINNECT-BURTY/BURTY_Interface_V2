/**
 *
 *
 * <pre>
 * <b>Description  : 인증 도메인 모델 (SocialAuthorizeUrlResult)</b>
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

/** OAuth 인가 URL과 CSRF 방지용 state(서버가 발급하거나 클라이언트가 전달한 값). */
public record SocialAuthorizeUrlResult(String authorizeUrl, String state) {}
