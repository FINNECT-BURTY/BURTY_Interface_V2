package com.burty.domain.repository;

import com.burty.domain.entity.CashflowScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CashflowScheduleRepository extends JpaRepository<CashflowScheduleEntity, Long> {
    List<CashflowScheduleEntity> findByUserIdAndActiveTrue(Long userId);
}
