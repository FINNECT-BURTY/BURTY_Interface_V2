/**
 *
 *
 * <pre>
 * <b>Description  : 정책 응답 DTO (PolicyApplyRateResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.dto.policy
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
package com.burty.application.dto.policy;

public record PolicyApplyRateResponse(
    String title, long matched, long applied, double applyRatePct) {}
