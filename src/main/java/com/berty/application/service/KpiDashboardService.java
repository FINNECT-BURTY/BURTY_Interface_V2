package com.berty.application.service;

import com.berty.application.port.in.KpiDashboardUseCase;
import com.berty.domain.entity.ActionFeedbackScoreEntity;
import com.berty.domain.entity.CashflowForecastEntity;
import com.berty.domain.entity.PolicyEntity;
import com.berty.domain.repository.ActionExecutionRepository;
import com.berty.domain.repository.ActionFeedbackRepository;
import com.berty.domain.repository.ActionFeedbackScoreRepository;
import com.berty.domain.repository.CashflowForecastHistoryRepository;
import com.berty.domain.repository.PolicyMatchLogRepository;
import com.berty.domain.repository.PolicyRepository;
import com.berty.domain.repository.RiskAssessmentHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

@Service
public class KpiDashboardService implements KpiDashboardUseCase {

    private final ActionExecutionRepository actionExecutionRepository;
    private final ActionFeedbackRepository actionFeedbackRepository;
    private final ActionFeedbackScoreRepository feedbackScoreRepository;
    private final CashflowForecastHistoryRepository forecastHistoryRepository;
    private final RiskAssessmentHistoryRepository riskHistoryRepository;
    private final PolicyMatchLogRepository policyMatchLogRepository;
    private final PolicyRepository policyRepository;

    public KpiDashboardService(ActionExecutionRepository actionExecutionRepository,
                               ActionFeedbackRepository actionFeedbackRepository,
                               ActionFeedbackScoreRepository feedbackScoreRepository,
                               CashflowForecastHistoryRepository forecastHistoryRepository,
                               RiskAssessmentHistoryRepository riskHistoryRepository,
                               PolicyMatchLogRepository policyMatchLogRepository,
                               PolicyRepository policyRepository) {
        this.actionExecutionRepository = actionExecutionRepository;
        this.actionFeedbackRepository = actionFeedbackRepository;
        this.feedbackScoreRepository = feedbackScoreRepository;
        this.forecastHistoryRepository = forecastHistoryRepository;
        this.riskHistoryRepository = riskHistoryRepository;
        this.policyMatchLogRepository = policyMatchLogRepository;
        this.policyRepository = policyRepository;
    }

    @Override
    public Map<String, Object> userKpi(String userId) {
        long executed = actionExecutionRepository.countByUserId(userId);
        long accepted = actionFeedbackRepository.countByUserIdAndFeedbackIgnoreCase(userId, "accept");
        long rejected = actionFeedbackRepository.countByUserIdAndFeedbackIgnoreCase(userId, "reject");
        double acceptRate = (accepted + rejected) == 0 ? 0.0 : Math.round(accepted * 1000.0 / (accepted + rejected)) / 10.0;

        List<CashflowForecastEntity> forecasts = forecastHistoryRepository.findTop30ByUserIdOrderByForecastDateDesc(userId);
        OptionalDouble avgAccuracy = forecasts.stream()
                .map(CashflowForecastEntity::getAccuracyPct)
                .filter(v -> v != null)
                .mapToDouble(Double::doubleValue)
                .average();

        long redCount = riskHistoryRepository.countByUserIdAndLevel(userId, "RED");
        long yellowCount = riskHistoryRepository.countByUserIdAndLevel(userId, "YELLOW");
        long greenCount = riskHistoryRepository.countByUserIdAndLevel(userId, "GREEN");

        List<ActionFeedbackScoreEntity> scoreRows = feedbackScoreRepository.findByUserId(userId);
        Map<String, Integer> topScores = new LinkedHashMap<>();
        scoreRows.stream()
                .sorted((a, b) -> Integer.compare(b.getScore(), a.getScore()))
                .limit(5)
                .forEach(s -> topScores.put(s.getActionTypeCode(), s.getScore()));

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("executedActions", executed);
        result.put("acceptedFeedback", accepted);
        result.put("rejectedFeedback", rejected);
        result.put("acceptRatePct", acceptRate);
        result.put("recentForecastCount", forecasts.size());
        result.put("avgAccuracyPct", avgAccuracy.isPresent() ? avgAccuracy.getAsDouble() : null);
        result.put("riskLevelCounts", Map.of("RED", redCount, "YELLOW", yellowCount, "GREEN", greenCount));
        result.put("topActionScores", topScores);
        return result;
    }

    @Override
    public Map<String, Object> globalKpi() {
        long executions = actionExecutionRepository.count();
        long forecasts = forecastHistoryRepository.count();
        long assessments = riskHistoryRepository.count();

        Map<String, Map<String, Object>> policyApplyRates = new LinkedHashMap<>();
        for (PolicyEntity policy : policyRepository.findByActiveTrue()) {
            long matched = policyMatchLogRepository.countByPolicyCode(policy.getPolicyCode());
            long applied = policyMatchLogRepository.countByPolicyCodeAndAppliedTrue(policy.getPolicyCode());
            double rate = matched == 0 ? 0.0 : Math.round(applied * 1000.0 / matched) / 10.0;
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("title", policy.getTitle());
            stat.put("matched", matched);
            stat.put("applied", applied);
            stat.put("applyRatePct", rate);
            policyApplyRates.put(policy.getPolicyCode(), stat);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalActionExecutions", executions);
        result.put("totalForecastSnapshots", forecasts);
        result.put("totalRiskAssessments", assessments);
        result.put("policyApplyRates", policyApplyRates);
        return result;
    }
}
