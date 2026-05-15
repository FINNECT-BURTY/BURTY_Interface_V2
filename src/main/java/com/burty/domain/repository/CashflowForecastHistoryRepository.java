package com.burty.domain.repository;

import com.burty.domain.entity.CashflowForecastEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CashflowForecastHistoryRepository extends JpaRepository<CashflowForecastEntity, Long> {
    Optional<CashflowForecastEntity> findByUserIdAndForecastDate(String userId, LocalDate forecastDate);

    List<CashflowForecastEntity> findTop30ByUserIdOrderByForecastDateDesc(String userId);

    List<CashflowForecastEntity> findByForecastDateAndAccuracyPctIsNull(LocalDate forecastDate);
}
