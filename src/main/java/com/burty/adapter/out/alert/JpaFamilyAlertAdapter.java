/**
 *
 *
 * <pre>
 * <b>Description  : 외부연동 외부 연동 어댑터 (JpaFamilyAlertAdapter)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.out.alert
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
package com.burty.adapter.out.alert;

import com.burty.application.port.out.notify.FamilyAlertPort;
import com.burty.domain.family.model.FamilyAlert;
import com.burty.domain.notification.entity.NotificationEntity;
import com.burty.domain.notification.repository.NotificationRepository;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class JpaFamilyAlertAdapter implements FamilyAlertPort {
  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final FamilyAlertSseBroker sseBroker;

  public JpaFamilyAlertAdapter(
      NotificationRepository notificationRepository,
      UserRepository userRepository,
      FamilyAlertSseBroker sseBroker) {
    this.notificationRepository = notificationRepository;
    this.userRepository = userRepository;
    this.sseBroker = sseBroker;
  }

  @Override
  public void send(String userId, String message) {
    Long userKey = parseUserKey(userId);
    if (userKey == null) return;
    UserEntity user = userRepository.findById(userKey).orElse(null);
    if (user == null) return;

    NotificationEntity entity = new NotificationEntity();
    entity.setRecipientUser(user);
    entity.setNotificationType(NotificationEntity.NotificationType.TRANSFER_ALERT);
    entity.setChannel(NotificationEntity.Channel.IN_APP);
    entity.setTitle("가족 보호 알림");
    entity.setBody(message);
    entity.setStatus(NotificationEntity.Status.QUEUED);
    entity.setSentAt(LocalDateTime.now());
    notificationRepository.save(entity);
    sseBroker.publish(new FamilyAlert(userId, message, entity.getSentAt()));
  }

  @Override
  public List<FamilyAlert> findByUserId(String userId) {
    Long userKey = parseUserKey(userId);
    if (userKey == null) return List.of();
    return notificationRepository
        .findByRecipientUser_UserIdOrderByNotificationIdDesc(userKey)
        .stream()
        .map(
            n ->
                new FamilyAlert(
                    userId,
                    n.getBody(),
                    n.getSentAt() == null ? LocalDateTime.now() : n.getSentAt()))
        .toList();
  }

  private Long parseUserKey(String userId) {
    try {
      return Long.parseLong(userId);
    } catch (Exception ignored) {
      return null;
    }
  }
}
