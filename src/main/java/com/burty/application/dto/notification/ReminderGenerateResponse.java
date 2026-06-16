/**
 *
 *
 * <pre>
 * <b>Description  : 알림 응답 DTO (ReminderGenerateResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.dto.notification
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
package com.burty.application.dto.notification;

public record ReminderGenerateResponse(String userId, int createdCount) {}
