package com.burty.domain.repository;

import com.burty.domain.entity.CashflowScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CashflowScheduleRepository extends JpaRepository<CashflowScheduleEntity, UUID> {
    List<CashflowScheduleEntity> findByUserIdAndActiveTrue(UUID userId);
}
