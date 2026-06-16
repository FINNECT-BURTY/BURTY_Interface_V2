/**
 *
 *
 * <pre>
 * <b>Description  : 현금흐름 애플리케이션 서비스 (RiskAssessmentService)</b>
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

import com.burty.application.port.in.admin.BaseCodeUseCase;
import com.burty.application.port.in.cashflow.CashflowForecastUseCase;
import com.burty.application.port.in.cashflow.RiskAssessmentUseCase;
import com.burty.application.service.support.AuditLogger;
import com.burty.core.code.CodeGroups;
import com.burty.core.constant.LogMessages;
import com.burty.domain.admin.entity.BaseCodeEntity;
import com.burty.domain.cashflow.entity.RiskAssessmentEntity;
import com.burty.domain.cashflow.model.CashflowForecast;
import com.burty.domain.cashflow.model.RiskAssessment;
import com.burty.domain.cashflow.repository.RiskAssessmentHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RiskAssessmentService implements RiskAssessmentUseCase {
  private static final Logger log = LoggerFactory.getLogger(RiskAssessmentService.class);
  private static final long DEFAULT_LOW = 50_000L;
  private static final long DEFAULT_NEGATIVE = 0L;

  private final CashflowForecastUseCase forecastUseCase;
  private final BaseCodeUseCase baseCodeUseCase;
  private final RiskAssessmentHistoryRepository historyRepository;
  private final AuditLogger auditLogger;

  public RiskAssessmentService(
      CashflowForecastUseCase forecastUseCase,
      BaseCodeUseCase baseCodeUseCase,
      RiskAssessmentHistoryRepository historyRepository,
      AuditLogger auditLogger) {
    this.forecastUseCase = forecastUseCase;
    this.baseCodeUseCase = baseCodeUseCase;
    this.historyRepository = historyRepository;
    this.auditLogger = auditLogger;
  }

  @Override
  public RiskAssessment assess(String userId) {
    CashflowForecast forecast = forecastUseCase.forecast(userId);
    long minBalance = forecast.minimumBalance();

    long lowThreshold = Math.max(thresholdFor("YELLOW", DEFAULT_LOW), forecast.safetyBalance());
    long negativeThreshold = thresholdFor("RED", DEFAULT_NEGATIVE);

    String level;
    long threshold;
    if (minBalance < negativeThreshold) {
      level = "RED";
      threshold = negativeThreshold;
    } else if (minBalance < lowThreshold) {
      level = "YELLOW";
      threshold = lowThreshold;
    } else {
      level = "GREEN";
      threshold = lowThreshold;
    }

    RiskAssessment assessment =
        new RiskAssessment(
            userId, level, threshold, forecast.riskReason(), forecast.riskDate(), minBalance);
    log.info(LogMessages.Cashflow.RISK_KPI, userId, level, minBalance);

    snapshot(userId, level, threshold, minBalance, forecast.riskDate(), forecast.riskReason());
    auditLogger.logSuccess(userId, "RISK_ASSESSMENT", level, "minBalance=" + minBalance);
    return assessment;
  }

  private void snapshot(
      String userId,
      String level,
      long threshold,
      long minBalance,
      java.time.LocalDate riskDate,
      String reason) {
    try {
      RiskAssessmentEntity entity = new RiskAssessmentEntity();
      entity.setUserId(userId);
      entity.setLevel(level);
      entity.setThresholdAmount(threshold);
      entity.setProjectedBalance(minBalance);
      entity.setRiskDate(riskDate);
      entity.setReason(reason);
      historyRepository.save(entity);
    } catch (Exception e) {
      log.warn("Risk snapshot save failed userId={} err={}", userId, e.getMessage());
    }
  }

  private long thresholdFor(String levelValue, long defaultValue) {
    return baseCodeUseCase
        .lookup(CodeGroups.RISK_LEVEL, levelValue)
        .map(BaseCodeEntity::getAttr2)
        .map(
            s -> {
              try {
                return Long.parseLong(s);
              } catch (NumberFormatException e) {
                return defaultValue;
              }
            })
        .orElse(defaultValue);
  }
}
