/**
 *
 *
 * <pre>
 * <b>Description  : 알림 유스케이스 포트 (NotificationManagementUseCase)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.in.notification
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
package com.burty.application.port.in.notification;

import com.burty.application.dto.notification.NotificationResponse;
import com.burty.application.dto.notification.ReminderGenerateResponse;
import java.util.List;

public interface NotificationManagementUseCase {

  List<NotificationResponse> notifications(String userId);

  ReminderGenerateResponse generateReminders(String userId);
}
