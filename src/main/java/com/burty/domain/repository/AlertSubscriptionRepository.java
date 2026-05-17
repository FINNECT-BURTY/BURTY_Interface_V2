package com.burty.domain.repository;

import com.burty.domain.entity.AlertSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertSubscriptionRepository extends JpaRepository<AlertSubscriptionEntity, UUID> {
    List<AlertSubscriptionEntity> findByGuardianLink_LinkId(UUID linkId);

    List<AlertSubscriptionEntity> findByGuardianLink_SeniorUser_UserId(Long seniorUserId);
}
