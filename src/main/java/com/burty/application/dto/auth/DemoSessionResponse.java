/**
 *
 *
 * <pre>
 * <b>Description  : 인증 응답 DTO (DemoSessionResponse)</b>
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

public record DemoSessionResponse(
    String userId, String accessToken, String persona, String scenario, String homeUrl) {}
