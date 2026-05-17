package com.burty.domain.repository;

import com.burty.domain.entity.AlertSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertSubscriptionRepository extends JpaRepository<AlertSubscriptionEntity, Long> {
    List<AlertSubscriptionEntity> findByGuardianLink_LinkId(Long linkId);

    List<AlertSubscriptionEntity> findByGuardianLink_SeniorUser_UserId(Long seniorUserId);
}
