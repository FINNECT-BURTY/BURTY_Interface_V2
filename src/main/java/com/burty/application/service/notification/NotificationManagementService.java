/**
 *
 *
 * <pre>
 * <b>Description  : 알림 애플리케이션 서비스 (NotificationManagementService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.notification
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
package com.burty.application.service.notification;

import com.burty.application.dto.notification.NotificationResponse;
import com.burty.application.dto.notification.ReminderGenerateResponse;
import com.burty.application.port.in.cashflow.CashflowForecastUseCase;
import com.burty.application.port.in.notification.NotificationManagementUseCase;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.cashflow.entity.CashflowScheduleEntity;
import com.burty.domain.cashflow.model.CashflowForecast;
import com.burty.domain.cashflow.repository.CashflowScheduleRepository;
import com.burty.domain.notification.entity.NotificationEntity;
import com.burty.domain.notification.repository.NotificationRepository;
import com.burty.domain.policy.entity.PolicyEntity;
import com.burty.domain.policy.repository.PolicyRepository;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationManagementService implements NotificationManagementUseCase {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final CashflowForecastUseCase cashflowForecastUseCase;
  private final CashflowScheduleRepository scheduleRepository;
  private final PolicyRepository policyRepository;

  @Override
  public List<NotificationResponse> notifications(String userId) {
    return notificationRepository
        .findByRecipientUser_UserIdOrderByNotificationIdDesc(Long.parseLong(userId))
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  public ReminderGenerateResponse generateReminders(String userId) {
    UserEntity user =
        userRepository
            .findById(Long.parseLong(userId))
            .orElseThrow(
                () -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다."));
    int created = 0;
    CashflowForecast forecast = cashflowForecastUseCase.forecast(userId);
    if (forecast.riskDate() != null) {
      long days = ChronoUnit.DAYS.between(LocalDate.now(), forecast.riskDate());
      if (days == 7 || days == 3 || days == 1 || days == 0) {
        save(
            user,
            NotificationEntity.NotificationType.CASHFLOW_RISK,
            "현금흐름 위험일이 다가옵니다",
            forecast.riskDate() + " 예상 잔액이 " + forecast.minimumBalance() + "원입니다.",
            "/cashflow/calendar");
        created++;
      }
    }
    Long numericUserId = Long.parseLong(userId);
    for (CashflowScheduleEntity schedule :
        scheduleRepository.findByUserIdAndActiveTrue(numericUserId)) {
      int daysUntil = daysUntil(schedule.getDayOfMonth());
      if (daysUntil > 3) continue;
      String label = schedule.getLabel() == null ? "" : schedule.getLabel();
      if (label.contains("카드") || schedule.getScheduleTypeCode().contains("CARD")) {
        save(
            user,
            NotificationEntity.NotificationType.CARD_DUE,
            "카드 결제일이 다가옵니다",
            label + " " + schedule.getAmount() + "원 예정입니다.",
            "/cashflow/calendar");
        created++;
      } else if (label.contains("월세") || schedule.getScheduleTypeCode().contains("RENT")) {
        save(
            user,
            NotificationEntity.NotificationType.RENT_DUE,
            "월세 납부일이 다가옵니다",
            label + " " + schedule.getAmount() + "원 예정입니다.",
            "/cashflow/calendar");
        created++;
      }
    }
    for (PolicyEntity policy : policyRepository.findByActiveTrue()) {
      if (policy.getValidTo() != null) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), policy.getValidTo());
        if (days >= 0 && days <= 7) {
          save(
              user,
              NotificationEntity.NotificationType.POLICY_DEADLINE,
              "정책 신청 마감이 가까워요",
              policy.getTitle() + " 신청 마감까지 " + days + "일 남았습니다.",
              "/policy/" + policy.getPolicyCode());
          created++;
        }
      }
    }
    return new ReminderGenerateResponse(userId, created);
  }

  private void save(
      UserEntity user,
      NotificationEntity.NotificationType type,
      String title,
      String body,
      String deepLink) {
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
      next =
          next.plusMonths(1)
              .withDayOfMonth(
                  Math.min(Math.max(1, dayOfMonth), next.plusMonths(1).lengthOfMonth()));
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
        entity.getReadAt());
  }
}
