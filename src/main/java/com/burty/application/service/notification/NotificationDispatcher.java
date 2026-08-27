/**
 *
 *
 * <pre>
 * <b>Description  : 알림 (NotificationDispatcher)</b>
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

import com.burty.application.port.out.notify.NotificationChannelPort;
import com.burty.application.port.out.queue.AsyncJobPort;
import com.burty.application.port.out.queue.AsyncJobType;
import com.burty.config.NotifyProperties;
import com.burty.domain.family.entity.AlertSubscriptionEntity;
import com.burty.domain.family.repository.AlertSubscriptionRepository;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Routes a notification to channels based on AlertSubscription.channel. Uses
 * NotificationChannelPort implementations registered as Spring beans.
 */
@Service
public class NotificationDispatcher {
  private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

  private final Map<NotificationChannelPort.Channel, NotificationChannelPort> channelMap;
  private final AlertSubscriptionRepository alertSubscriptionRepository;
  private final ObjectProvider<AsyncJobPort> asyncJobPort;
  private final NotifyProperties notifyProperties;

  public NotificationDispatcher(
      List<NotificationChannelPort> ports,
      AlertSubscriptionRepository alertSubscriptionRepository,
      ObjectProvider<AsyncJobPort> asyncJobPort,
      NotifyProperties notifyProperties) {
    this.channelMap = new EnumMap<>(NotificationChannelPort.Channel.class);
    for (NotificationChannelPort port : ports) {
      this.channelMap.put(port.channel(), port);
    }
    this.alertSubscriptionRepository = alertSubscriptionRepository;
    this.asyncJobPort = asyncJobPort;
    this.notifyProperties = notifyProperties;
  }

  /**
   * Dispatches to the channel preferred by the user's active subscription. If no subscription
   * exists, defaults to PUSH.
   */
  public boolean dispatchPreferred(String userId, String title, String body) {
    NotificationChannelPort.Channel channel = resolvePreferredChannel(userId);
    return dispatch(userId, channel, title, body);
  }

  /** Direct dispatch to a specific channel. */
  public boolean dispatch(
      String userId, NotificationChannelPort.Channel channel, String title, String body) {
    AsyncJobPort queue = asyncJobPort.getIfAvailable();
    if (notifyProperties.isAsyncEnabled() && queue != null && queue.isEnabled()) {
      Map<String, String> payload = new HashMap<>();
      payload.put("userId", userId);
      payload.put("channel", channel.name());
      payload.put("title", title != null ? title : "");
      payload.put("body", body != null ? body : "");
      queue.publish(AsyncJobType.NOTIFICATION, payload);
      return true;
    }
    return dispatchDirect(userId, channel, title, body);
  }

  /**
   * 즉시 발송. 실패하면 <b>예외를 던진다.</b>
   *
   * <p>예전에는 {@code false} 를 반환했고, 호출부(큐 컨슈머)는 그 값을 보지도 않고 ACK 했다. 실패가 신호로 전달되지 않으면 재시도할 수 없다.
   */
  public boolean dispatchDirect(
      String userId, NotificationChannelPort.Channel channel, String title, String body) {
    NotificationChannelPort port = channelMap.get(channel);
    if (port == null) {
      throw new IllegalStateException("등록된 알림 채널 어댑터가 없습니다: " + channel);
    }
    boolean sent = port.send(userId, title, body);
    if (!sent) {
      throw new NotificationDeliveryException("알림 발송 실패 channel=" + channel + " userId=" + userId);
    }
    return true;
  }

  /** 발송 실패. 아웃박스/큐가 재시도 여부를 판단하는 신호다. */
  public static class NotificationDeliveryException extends RuntimeException {
    public NotificationDeliveryException(String message) {
      super(message);
    }
  }

  public NotificationChannelPort.Channel resolvePreferredChannel(String userId) {
    try {
      Long userKey = Long.parseLong(userId);
      return alertSubscriptionRepository.findByGuardianLink_SeniorUser_UserId(userKey).stream()
          .filter(s -> Boolean.TRUE.equals(s.getActive()))
          .findFirst()
          .map(AlertSubscriptionEntity::getChannel)
          .map(this::mapEnum)
          .orElse(NotificationChannelPort.Channel.PUSH);
    } catch (NumberFormatException e) {
      return NotificationChannelPort.Channel.PUSH;
    }
  }

  private NotificationChannelPort.Channel mapEnum(AlertSubscriptionEntity.Channel ch) {
    return switch (ch) {
      case SMS -> NotificationChannelPort.Channel.SMS;
      case EMAIL -> NotificationChannelPort.Channel.EMAIL;
      case PUSH, ALL -> NotificationChannelPort.Channel.PUSH;
    };
  }
}
