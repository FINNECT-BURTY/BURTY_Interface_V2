package com.burty.domain.repository;

import com.burty.domain.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findByRecipientUser_UserIdOrderByNotificationIdDesc(UUID userId);
}
