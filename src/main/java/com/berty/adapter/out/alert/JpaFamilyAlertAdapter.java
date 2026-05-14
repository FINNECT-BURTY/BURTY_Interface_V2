package com.berty.adapter.out.alert;

import com.berty.application.port.out.FamilyAlertPort;
import com.berty.domain.entity.NotificationEntity;
import com.berty.domain.entity.UserEntity;
import com.berty.domain.model.FamilyAlert;
import com.berty.domain.repository.NotificationRepository;
import com.berty.domain.repository.UserRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Primary
@Component
public class JpaFamilyAlertAdapter implements FamilyAlertPort {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final FamilyAlertSseBroker sseBroker;

    public JpaFamilyAlertAdapter(NotificationRepository notificationRepository, UserRepository userRepository, FamilyAlertSseBroker sseBroker) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.sseBroker = sseBroker;
    }

    @Override
    public void send(String userId, String message) {
        UUID uuid = parseUuid(userId);
        if (uuid == null) return;
        UserEntity user = userRepository.findById(uuid).orElse(null);
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
        UUID uuid = parseUuid(userId);
        if (uuid == null) return List.of();
        return notificationRepository.findByRecipientUser_UserIdOrderByNotificationIdDesc(uuid).stream()
                .map(n -> new FamilyAlert(userId, n.getBody(), n.getSentAt() == null ? LocalDateTime.now() : n.getSentAt()))
                .toList();
    }

    private UUID parseUuid(String userId) {
        try {
            return UUID.fromString(userId);
        } catch (Exception ignored) {
            return null;
        }
    }
}
