/**
 *
 *
 * <pre>
 * <b>Description  : 현금흐름 (CashflowForecastSnapshotter)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.cashflow
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
package com.burty.application.service.cashflow;

import com.burty.domain.cashflow.entity.CashflowForecastEntity;
import com.burty.domain.cashflow.repository.CashflowForecastHistoryRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CashflowForecastSnapshotter {

  private static final Logger log = LoggerFactory.getLogger(CashflowForecastSnapshotter.class);

  private final CashflowForecastHistoryRepository historyRepository;

  public void snapshot(
      String userId,
      LocalDate forecastDate,
      long openingBalance,
      long minimumBalance,
      LocalDate riskDate,
      String riskReason) {
    try {
      CashflowForecastEntity entity =
          historyRepository
              .findByUserIdAndForecastDate(userId, forecastDate)
              .orElseGet(CashflowForecastEntity::new);
      entity.setUserId(userId);
      entity.setForecastDate(forecastDate);
      entity.setOpeningBalance(openingBalance);
      entity.setMinimumBalance(minimumBalance);
      entity.setRiskDate(riskDate);
      entity.setRiskReason(riskReason);
      entity.setHorizonDays(30);
      historyRepository.save(entity);
    } catch (Exception e) {
      log.warn("Forecast snapshot save failed userId={} err={}", userId, e.getMessage());
    }
  }
}
