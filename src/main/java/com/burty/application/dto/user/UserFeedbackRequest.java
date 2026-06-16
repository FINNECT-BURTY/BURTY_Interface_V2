/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 요청 DTO (UserFeedbackRequest)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.dto.user
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
package com.burty.application.dto.user;

public record UserFeedbackRequest(
    String userId, String targetType, String targetId, String feedbackType, String feedbackValue) {}
