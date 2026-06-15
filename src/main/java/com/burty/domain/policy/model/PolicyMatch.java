/**
 *
 *
 * <pre>
 * <b>Description  : 정책 도메인 모델 (PolicyMatch)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.policy.model
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
package com.burty.domain.policy.model;

public record PolicyMatch(
    String policyId, String policyName, String supportType, String reason, int priorityScore) {}
