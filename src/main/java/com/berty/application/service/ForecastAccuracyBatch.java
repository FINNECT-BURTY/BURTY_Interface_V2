package com.berty.application.service;

import com.berty.application.port.in.CashflowForecastUseCase;
import com.berty.domain.entity.CashflowForecastEntity;
import com.berty.domain.model.CashflowForecast;
import com.berty.domain.repository.CashflowForecastHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class ForecastAccuracyBatch {
    private static final Logger log = LoggerFactory.getLogger(ForecastAccuracyBatch.class);

    private final CashflowForecastHistoryRepository historyRepository;
    private final CashflowForecastUseCase forecastUseCase;

    public ForecastAccuracyBatch(CashflowForecastHistoryRepository historyRepository,
                                 CashflowForecastUseCase forecastUseCase) {
        this.historyRepository = historyRepository;
        this.forecastUseCase = forecastUseCase;
    }

    @Scheduled(cron = "${berty.forecast.accuracy-cron:0 5 1 * * *}")
    @Transactional
    public void computeYesterdayAccuracy() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<CashflowForecastEntity> pending = historyRepository.findByForecastDateAndAccuracyPctIsNull(yesterday);
        if (pending.isEmpty()) {
            log.debug("Forecast accuracy batch: no pending records for {}", yesterday);
            return;
        }
        int updated = 0;
        for (CashflowForecastEntity entity : pending) {
            try {
                CashflowForecast current = forecastUseCase.forecast(entity.getUserId());
                long actual = current.getMinimumBalance();
                long predicted = entity.getMinimumBalance();
                long denominator = Math.max(Math.abs(predicted), Math.abs(actual));
                double accuracy = denominator == 0
                        ? 100.0
                        : Math.max(0.0, 100.0 - Math.abs(predicted - actual) * 100.0 / denominator);
                entity.setActualMinBalance(actual);
                entity.setAccuracyPct(Math.round(accuracy * 10.0) / 10.0);
                historyRepository.save(entity);
                updated++;
            } catch (Exception e) {
                log.warn("Accuracy compute failed userId={} forecastDate={} err={}",
                        entity.getUserId(), entity.getForecastDate(), e.getMessage());
            }
        }
        log.info("Forecast accuracy batch: forecastDate={} updated={}/{}", yesterday, updated, pending.size());
    }
}
