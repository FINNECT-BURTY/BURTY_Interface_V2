package com.berty.adapter.in.web;

import com.berty.adapter.in.web.dto.NotificationResponse;
import com.berty.adapter.in.web.dto.ReminderGenerateResponse;
import com.berty.core.dto.response.ApiResponse;
import com.berty.domain.entity.CashflowScheduleEntity;
import com.berty.domain.entity.NotificationEntity;
import com.berty.domain.entity.PolicyEntity;
import com.berty.domain.entity.UserEntity;
import com.berty.domain.model.CashflowForecast;
import com.berty.domain.repository.CashflowScheduleRepository;
import com.berty.domain.repository.NotificationRepository;
import com.berty.domain.repository.PolicyRepository;
import com.berty.domain.repository.UserRepository;
import com.berty.application.port.in.CashflowForecastUseCase;
import com.berty.security.AuthLevel;
import com.berty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/berty/notifications")
@Tag(name = "BERTY Notifications", description = "위험일/결제일/정책 마감 알림 API")
public class NotificationManagementController {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CashflowForecastUseCase cashflowForecastUseCase;
    private final CashflowScheduleRepository scheduleRepository;
    private final PolicyRepository policyRepository;

    public NotificationManagementController(NotificationRepository notificationRepository,
                                            UserRepository userRepository,
                                            CashflowForecastUseCase cashflowForecastUseCase,
                                            CashflowScheduleRepository scheduleRepository,
                                            PolicyRepository policyRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.cashflowForecastUseCase = cashflowForecastUseCase;
        this.scheduleRepository = scheduleRepository;
        this.policyRepository = policyRepository;
    }

    @GetMapping
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<List<NotificationResponse>> notifications(@RequestParam String userId) {
        return ApiResponse.ok(notificationRepository.findByRecipientUser_UserIdOrderByNotificationIdDesc(UUID.fromString(userId))
                .stream()
                .map(this::toResponse)
                .toList());
    }

    @PostMapping("/generate-reminders")
    @AuthLevel(RiskLevel.LEVEL_2)
    @Operation(summary = "알림 생성", description = "위험일 D-7/D-3/D-1, 카드/월세 납부 전, 정책 마감 전 알림을 생성합니다.")
    public ApiResponse<ReminderGenerateResponse> generateReminders(@RequestParam String userId) {
        UserEntity user = userRepository.findById(UUID.fromString(userId)).orElseThrow();
        int created = 0;
        CashflowForecast forecast = cashflowForecastUseCase.forecast(userId);
        if (forecast.getRiskDate() != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), forecast.getRiskDate());
            if (days == 7 || days == 3 || days == 1 || days == 0) {
                save(user, NotificationEntity.NotificationType.CASHFLOW_RISK, "현금흐름 위험일이 다가와요",
                        forecast.getRiskDate() + " 예상 잔액이 " + forecast.getMinimumBalance() + "원입니다.",
                        "/cashflow/calendar");
                created++;
            }
        }
        UUID uuid = UUID.fromString(userId);
        for (CashflowScheduleEntity schedule : scheduleRepository.findByUserIdAndActiveTrue(uuid)) {
            int daysUntil = daysUntil(schedule.getDayOfMonth());
            if (daysUntil > 3) continue;
            String label = schedule.getLabel() == null ? "" : schedule.getLabel();
            if (label.contains("카드") || schedule.getScheduleTypeCode().contains("CARD")) {
                save(user, NotificationEntity.NotificationType.CARD_DUE, "카드 결제일이 다가와요", label + " " + schedule.getAmount() + "원 예정입니다.", "/cashflow/calendar");
                created++;
            } else if (label.contains("월세") || schedule.getScheduleTypeCode().contains("RENT")) {
                save(user, NotificationEntity.NotificationType.RENT_DUE, "월세 납부일이 다가와요", label + " " + schedule.getAmount() + "원 예정입니다.", "/cashflow/calendar");
                created++;
            }
        }
        for (PolicyEntity policy : policyRepository.findByActiveTrue()) {
            if (policy.getValidTo() != null) {
                long days = ChronoUnit.DAYS.between(LocalDate.now(), policy.getValidTo());
                if (days >= 0 && days <= 7) {
                    save(user, NotificationEntity.NotificationType.POLICY_DEADLINE, "정책 신청 마감이 가까워요",
                            policy.getTitle() + " 신청 마감까지 " + days + "일 남았습니다.", "/policy/" + policy.getPolicyCode());
                    created++;
                }
            }
        }
        return ApiResponse.ok(new ReminderGenerateResponse(userId, created));
    }

    private void save(UserEntity user, NotificationEntity.NotificationType type, String title, String body, String deepLink) {
        NotificationEntity entity = new NotificationEntity();
        entity.setRecipientUser(user);
        entity.setNotificationType(type);
        entity.setChannel(NotificationEntity.Channel.IN_APP);
        entity.setTitle(title);
        entity.setBody(body);
        entity.setDeepLink(deepLink);
        entity.setStatus(NotificationEntity.Status.QUEUED);
        notificationRepository.save(entity);
    }

    private int daysUntil(int dayOfMonth) {
        LocalDate today = LocalDate.now();
        LocalDate next = today.withDayOfMonth(Math.min(Math.max(1, dayOfMonth), today.lengthOfMonth()));
        if (next.isBefore(today)) {
            next = next.plusMonths(1).withDayOfMonth(Math.min(Math.max(1, dayOfMonth), next.plusMonths(1).lengthOfMonth()));
        }
        return (int) ChronoUnit.DAYS.between(today, next);
    }

    private NotificationResponse toResponse(NotificationEntity entity) {
        return new NotificationResponse(
                entity.getNotificationId(),
                entity.getNotificationType().name(),
                entity.getChannel().name(),
                entity.getTitle(),
                entity.getBody(),
                entity.getDeepLink(),
                entity.getStatus().name(),
                entity.getSentAt(),
                entity.getReadAt()
        );
    }
}
