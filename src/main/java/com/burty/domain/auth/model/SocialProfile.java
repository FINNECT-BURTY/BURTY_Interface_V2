/**
 *
 *
 * <pre>
 * <b>Description  : 인증 도메인 모델 (SocialProfile)</b>
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

/**
 * OAuth provider 가 반환한 사용자 식별 정보. providerUserId 만 필수, email/displayName 은 동의/검수 여부에 따라 null 일 수
 * 있음.
 */
public record SocialProfile(String providerUserId, String email, String displayName) {}
