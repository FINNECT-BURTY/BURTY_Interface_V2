/**
 *
 *
 * <pre>
 * <b>Description  : 행동추천 응답 DTO (ActionExecutionResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.dto.action
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
package com.burty.application.dto.action;

public record ActionExecutionResponse(
    String userId, String actionType, boolean executed, String message) {}
