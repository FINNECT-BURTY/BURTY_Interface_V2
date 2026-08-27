package com.burty.application.service.notification;

import com.burty.application.port.out.notify.NotificationChannelPort;
import com.burty.application.port.out.outbox.OutboxEventHandler;
import com.burty.domain.notification.entity.NotificationEntity;
import com.burty.domain.notification.repository.NotificationRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 실제 발송 + 발송 이력 갱신.
 *
 * <p>예전에는 알림이 {@code tbl_notification} 에 {@code QUEUED} 로 저장되기만 하고 상태가 영원히 그대로였다. 즉 "보냈다" 는 사실이
 * 로그에만 남고, 실패했는지 몇 번 시도했는지 조회할 방법이 없었다. 아웃박스가 재시도를 책임지고, 여기서 시도 횟수와 결과를 이력에 남긴다.
 */
@Component
public class NotificationOutboxHandler implements OutboxEventHandler {

  public static final String EVENT_TYPE = "NotificationRequested";

  private static final Logger log = LoggerFactory.getLogger(NotificationOutboxHandler.class);

  private final NotificationRepository notificationRepository;
  private final NotificationDispatcher dispatcher;
  private final Clock clock;

  public NotificationOutboxHandler(
      NotificationRepository notificationRepository,
      NotificationDispatcher dispatcher,
      Clock clock) {
    this.notificationRepository = notificationRepository;
    this.dispatcher = dispatcher;
    this.clock = clock;
  }

  @Override
  public String eventType() {
    return EVENT_TYPE;
  }

  @Override
  @Transactional
  public void handle(String aggregateId, Map<String, Object> payload) {
    Long notificationId = Long.valueOf(aggregateId);
    NotificationEntity notification =
        notificationRepository
            .findById(notificationId)
            .orElseThrow(() -> new IllegalStateException("알림 이력을 찾을 수 없습니다 id=" + notificationId));

    if (notification.getStatus() == NotificationEntity.Status.SENT
        || notification.getStatus() == NotificationEntity.Status.DELIVERED
        || notification.getStatus() == NotificationEntity.Status.READ) {
      // 아웃박스는 at-least-once 다. 이미 보낸 건은 조용히 건너뛴다 (멱등).
      log.debug("이미 발송된 알림 — 재발송 생략 id={}", notificationId);
      return;
    }

    String userId = String.valueOf(payload.get("userId"));
    notification.setAttempts(
        (notification.getAttempts() == null ? 0 : notification.getAttempts()) + 1);

    NotificationChannelPort.Channel channel = dispatcher.resolvePreferredChannel(userId);
    notification.setDeliveryChannel(NotificationEntity.Channel.valueOf(channel.name()));

    try {
      dispatcher.dispatchDirect(userId, channel, notification.getTitle(), notification.getBody());
      notification.setStatus(NotificationEntity.Status.SENT);
      notification.setSentAt(LocalDateTime.now(clock));
      notification.setFailedReason(null);
    } catch (RuntimeException e) {
      notification.setStatus(NotificationEntity.Status.FAILED);
      notification.setFailedReason(truncate(e.getMessage()));
      // 다시 던져야 아웃박스가 재시도한다. 여기서 삼키면 알림이 조용히 사라진다.
      throw e;
    }
  }

  private static String truncate(String value) {
    if (value == null) {
      return null;
    }
    return value.length() <= 255 ? value : value.substring(0, 255);
  }
}
