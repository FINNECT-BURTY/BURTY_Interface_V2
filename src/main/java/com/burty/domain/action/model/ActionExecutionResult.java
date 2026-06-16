/**
 *
 *
 * <pre>
 * <b>Description  : 행동추천 도메인 모델 (ActionExecutionResult)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.action.model
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
package com.burty.domain.action.model;

public record ActionExecutionResult(
    String userId, String actionType, boolean executed, String message) {}
