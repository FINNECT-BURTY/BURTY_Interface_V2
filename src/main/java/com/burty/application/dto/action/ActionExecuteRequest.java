/**
 *
 *
 * <pre>
 * <b>Description  : 행동추천 요청 DTO (ActionExecuteRequest)</b>
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

public record ActionExecuteRequest(String userId, String actionType) {}
