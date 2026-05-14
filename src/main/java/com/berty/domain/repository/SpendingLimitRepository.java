package com.berty.domain.repository;

import com.berty.domain.entity.SpendingLimitEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpendingLimitRepository extends JpaRepository<SpendingLimitEntity, UUID> {
    List<SpendingLimitEntity> findByUser_UserId(UUID userId);

    List<SpendingLimitEntity> findByUser_UserIdAndPeriodType(UUID userId, SpendingLimitEntity.PeriodType periodType);
}
