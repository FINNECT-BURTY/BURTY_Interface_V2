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
import com.burty.domain.family.entity.AlertSubscriptionEntity;
import com.burty.domain.family.repository.AlertSubscriptionRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  public NotificationDispatcher(
      List<NotificationChannelPort> ports,
      AlertSubscriptionRepository alertSubscriptionRepository) {
    this.channelMap = new EnumMap<>(NotificationChannelPort.Channel.class);
    for (NotificationChannelPort port : ports) {
      this.channelMap.put(port.channel(), port);
    }
    this.alertSubscriptionRepository = alertSubscriptionRepository;
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
    NotificationChannelPort port = channelMap.get(channel);
    if (port == null) {
      log.warn("No adapter registered for channel={}, dropping userId={}", channel, userId);
      return false;
    }
    return port.send(userId, title, body);
  }

  private NotificationChannelPort.Channel resolvePreferredChannel(String userId) {
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
