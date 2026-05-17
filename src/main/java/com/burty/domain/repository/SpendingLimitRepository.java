package com.burty.domain.repository;

import com.burty.domain.entity.SpendingLimitEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpendingLimitRepository extends JpaRepository<SpendingLimitEntity, Long> {
    List<SpendingLimitEntity> findByUser_UserId(Long userId);

    List<SpendingLimitEntity> findByUser_UserIdAndPeriodType(Long userId, SpendingLimitEntity.PeriodType periodType);
}
