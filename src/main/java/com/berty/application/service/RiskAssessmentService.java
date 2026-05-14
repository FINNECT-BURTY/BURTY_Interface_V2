package com.berty.application.service;

import com.berty.application.port.in.BaseCodeUseCase;
import com.berty.application.port.in.CashflowForecastUseCase;
import com.berty.application.port.in.RiskAssessmentUseCase;
import com.berty.core.code.CodeGroups;
import com.berty.domain.entity.BaseCodeEntity;
import com.berty.domain.entity.RiskAssessmentEntity;
import com.berty.domain.model.CashflowForecast;
import com.berty.domain.model.RiskAssessment;
import com.berty.domain.repository.RiskAssessmentHistoryRepository;
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

    public RiskAssessmentService(CashflowForecastUseCase forecastUseCase,
                                 BaseCodeUseCase baseCodeUseCase,
                                 RiskAssessmentHistoryRepository historyRepository) {
        this.forecastUseCase = forecastUseCase;
        this.baseCodeUseCase = baseCodeUseCase;
        this.historyRepository = historyRepository;
    }

    @Override
    public RiskAssessment assess(String userId) {
        CashflowForecast forecast = forecastUseCase.forecast(userId);
        long minBalance = forecast.getMinimumBalance();

        long lowThreshold = Math.max(thresholdFor("YELLOW", DEFAULT_LOW), forecast.getSafetyBalance());
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

        RiskAssessment assessment = new RiskAssessment(
                userId, level, threshold, forecast.getRiskReason(), forecast.getRiskDate(), minBalance
        );
        log.info("KPI risk userId={} level={} projectedBalance={}", userId, level, minBalance);

        snapshot(userId, level, threshold, minBalance, forecast.getRiskDate(), forecast.getRiskReason());
        return assessment;
    }

    private void snapshot(String userId, String level, long threshold, long minBalance,
                          java.time.LocalDate riskDate, String reason) {
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
        return baseCodeUseCase.lookup(CodeGroups.RISK_LEVEL, levelValue)
                .map(BaseCodeEntity::getAttr2)
                .map(s -> {
                    try { return Long.parseLong(s); } catch (NumberFormatException e) { return defaultValue; }
                })
                .orElse(defaultValue);
    }
}
