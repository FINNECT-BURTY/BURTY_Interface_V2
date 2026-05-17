package com.burty.domain.repository;

import com.burty.domain.entity.MonthlyReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MonthlyReportRepository extends JpaRepository<MonthlyReportEntity, Long> {
    Optional<MonthlyReportEntity> findByUser_UserIdAndPeriodMonth(Long userId, LocalDate periodMonth);
}
