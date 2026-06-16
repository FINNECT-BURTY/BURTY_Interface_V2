/**
 *
 *
 * <pre>
 * <b>Description  : 알림 API 컨트롤러 (NotificationManagementController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.notification
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
package com.burty.adapter.in.web.notification;

import com.burty.application.dto.notification.NotificationResponse;
import com.burty.application.dto.notification.ReminderGenerateResponse;
import com.burty.application.port.in.notification.NotificationManagementUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "BURTY Notifications", description = "위험일/결제일/정책 마감 알림 API")
public class NotificationManagementController extends BaseController {

  private final NotificationManagementUseCase notificationManagementUseCase;

  @GetMapping
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<List<NotificationResponse>> notifications(@RequestParam String userId) {
    return ApiResponse.ok(notificationManagementUseCase.notifications(userId));
  }

  @PostMapping("/generate-reminders")
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "알림 생성", description = "위험일 D-7/D-3/D-1, 카드/월세 납부 전, 정책 마감 전 알림을 생성합니다.")
  public ApiResponse<ReminderGenerateResponse> generateReminders(@RequestParam String userId) {
    return ApiResponse.ok(notificationManagementUseCase.generateReminders(userId));
  }
}
