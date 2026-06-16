/**
 *
 *
 * <pre>
 * <b>Description  : 보안 (OAuthStateContext)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.security.oauth
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
package com.burty.security.oauth;

/** {@link OAuthStateStore#verifyAndConsume} 성공 시 authorize 단계에서 저장한 컨텍스트. */
public record OAuthStateContext(String frontendOrigin) {}
