/**
 *
 *
 * <pre>
 * <b>Description  : 현금흐름 애플리케이션 서비스 (KpiDashboardService)</b>
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

import com.burty.application.dto.cashflow.GlobalKpiResponse;
import com.burty.application.dto.cashflow.RiskLevelCountsResponse;
import com.burty.application.dto.cashflow.UserKpiResponse;
import com.burty.application.dto.policy.PolicyApplyRateResponse;
import com.burty.application.port.in.cashflow.KpiDashboardUseCase;
import com.burty.domain.action.entity.ActionFeedbackScoreEntity;
import com.burty.domain.action.repository.ActionExecutionRepository;
import com.burty.domain.action.repository.ActionFeedbackRepository;
import com.burty.domain.action.repository.ActionFeedbackScoreRepository;
import com.burty.domain.cashflow.entity.CashflowForecastEntity;
import com.burty.domain.cashflow.repository.CashflowForecastHistoryRepository;
import com.burty.domain.cashflow.repository.RiskAssessmentHistoryRepository;
import com.burty.domain.policy.entity.PolicyEntity;
import com.burty.domain.policy.repository.PolicyMatchLogRepository;
import com.burty.domain.policy.repository.PolicyRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import org.springframework.stereotype.Service;

@Service
public class KpiDashboardService implements KpiDashboardUseCase {

  private final ActionExecutionRepository actionExecutionRepository;
  private final ActionFeedbackRepository actionFeedbackRepository;
  private final ActionFeedbackScoreRepository feedbackScoreRepository;
  private final CashflowForecastHistoryRepository forecastHistoryRepository;
  private final RiskAssessmentHistoryRepository riskHistoryRepository;
  private final PolicyMatchLogRepository policyMatchLogRepository;
  private final PolicyRepository policyRepository;

  public KpiDashboardService(
      ActionExecutionRepository actionExecutionRepository,
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
  public UserKpiResponse userKpi(String userId) {
    long executed = actionExecutionRepository.countByUserId(userId);
    long accepted = actionFeedbackRepository.countByUserIdAndFeedbackIgnoreCase(userId, "accept");
    long rejected = actionFeedbackRepository.countByUserIdAndFeedbackIgnoreCase(userId, "reject");
    double acceptRate =
        (accepted + rejected) == 0
            ? 0.0
            : Math.round(accepted * 1000.0 / (accepted + rejected)) / 10.0;

    List<CashflowForecastEntity> forecasts =
        forecastHistoryRepository.findTop30ByUserIdOrderByForecastDateDesc(userId);
    OptionalDouble avgAccuracy =
        forecasts.stream()
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

    return new UserKpiResponse(
        userId,
        executed,
        accepted,
        rejected,
        acceptRate,
        forecasts.size(),
        avgAccuracy.isPresent() ? avgAccuracy.getAsDouble() : null,
        new RiskLevelCountsResponse(redCount, yellowCount, greenCount),
        topScores);
  }

  @Override
  public GlobalKpiResponse globalKpi() {
    long executions = actionExecutionRepository.count();
    long forecasts = forecastHistoryRepository.count();
    long assessments = riskHistoryRepository.count();

    Map<String, PolicyApplyRateResponse> policyApplyRates = new LinkedHashMap<>();
    for (PolicyEntity policy : policyRepository.findByActiveTrue()) {
      long matched = policyMatchLogRepository.countByPolicyCode(policy.getPolicyCode());
      long applied =
          policyMatchLogRepository.countByPolicyCodeAndAppliedTrue(policy.getPolicyCode());
      double rate = matched == 0 ? 0.0 : Math.round(applied * 1000.0 / matched) / 10.0;
      policyApplyRates.put(
          policy.getPolicyCode(),
          new PolicyApplyRateResponse(policy.getTitle(), matched, applied, rate));
    }

    return new GlobalKpiResponse(executions, forecasts, assessments, policyApplyRates);
  }
}
