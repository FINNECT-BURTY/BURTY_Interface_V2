package com.berty.domain.repository;

import com.berty.domain.entity.MonthlyReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface MonthlyReportRepository extends JpaRepository<MonthlyReportEntity, UUID> {
    Optional<MonthlyReportEntity> findByUser_UserIdAndPeriodMonth(UUID userId, LocalDate periodMonth);
}
