/**
 *
 *
 * <pre>
 * <b>Description  : 배치 배치 작업 (ForecastAccuracyBatch)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.batch
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.application.service.batch;

import com.burty.application.port.in.cashflow.CashflowForecastUseCase;
import com.burty.core.constant.LogMessages;
import com.burty.domain.cashflow.entity.CashflowForecastEntity;
import com.burty.domain.cashflow.model.CashflowForecast;
import com.burty.domain.cashflow.repository.CashflowForecastHistoryRepository;
import java.time.LocalDate;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ForecastAccuracyBatch {
  private static final Logger log = LoggerFactory.getLogger(ForecastAccuracyBatch.class);

  private final CashflowForecastHistoryRepository historyRepository;
  private final CashflowForecastUseCase forecastUseCase;

  public ForecastAccuracyBatch(
      CashflowForecastHistoryRepository historyRepository,
      CashflowForecastUseCase forecastUseCase) {
    this.historyRepository = historyRepository;
    this.forecastUseCase = forecastUseCase;
  }

  @Scheduled(cron = "${burty.forecast.accuracy-cron:0 5 1 * * *}")
  @SchedulerLock(name = "ForecastAccuracyBatch", lockAtLeastFor = "PT5M", lockAtMostFor = "PT55M")
  @Transactional
  public void computeYesterdayAccuracy() {
    LocalDate yesterday = LocalDate.now().minusDays(1);
    List<CashflowForecastEntity> pending =
        historyRepository.findByForecastDateAndAccuracyPctIsNull(yesterday);
    if (pending.isEmpty()) {
      log.debug("Forecast accuracy batch: no pending records for {}", yesterday);
      return;
    }
    int updated = 0;
    for (CashflowForecastEntity entity : pending) {
      try {
        CashflowForecast current = forecastUseCase.forecast(entity.getUserId());
        long actual = current.minimumBalance();
        long predicted = entity.getMinimumBalance();
        long denominator = Math.max(Math.abs(predicted), Math.abs(actual));
        double accuracy =
            denominator == 0
                ? 100.0
                : Math.max(0.0, 100.0 - Math.abs(predicted - actual) * 100.0 / denominator);
        entity.setActualMinBalance(actual);
        entity.setAccuracyPct(Math.round(accuracy * 10.0) / 10.0);
        historyRepository.save(entity);
        updated++;
      } catch (Exception e) {
        log.warn(
            "Accuracy compute failed userId={} forecastDate={} err={}",
            entity.getUserId(),
            entity.getForecastDate(),
            e.getMessage());
      }
    }
    log.info(LogMessages.Batch.FORECAST_ACCURACY, yesterday, updated, pending.size());
  }
}
