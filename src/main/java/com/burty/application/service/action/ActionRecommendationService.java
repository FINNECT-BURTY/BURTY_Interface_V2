/**
 *
 *
 * <pre>
 * <b>Description  : 행동추천 애플리케이션 서비스 (ActionRecommendationService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.action
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
package com.burty.application.service.action;

import com.burty.application.port.in.action.ActionRecommendationUseCase;
import com.burty.application.port.in.cashflow.CashflowForecastUseCase;
import com.burty.application.port.in.user.PersonaInferenceUseCase;
import com.burty.application.port.out.ai.EasyReadPort;
import com.burty.core.constant.LogMessages;
import com.burty.domain.action.entity.ActionRecommendationEntity;
import com.burty.domain.action.model.ActionExecutionResult;
import com.burty.domain.action.model.ActionFeedbackSummary;
import com.burty.domain.action.model.ActionRecommendation;
import com.burty.domain.action.repository.ActionRecommendationRepository;
import com.burty.domain.cashflow.model.CashflowForecast;
import com.burty.domain.cashflow.model.RecurringExpense;
import com.burty.domain.user.entity.PersonaProfileEntity;
import com.burty.util.BanditScorer;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActionRecommendationService implements ActionRecommendationUseCase {

  private static final Logger log = LoggerFactory.getLogger(ActionRecommendationService.class);

  private final CashflowForecastUseCase forecastUseCase;
  private final ActionRecommendationRepository recommendationRepository;
  private final PersonaInferenceUseCase personaInferenceUseCase;
  private final EasyReadPort easyReadPort;
  private final BanditScorer banditScorer;
  private final RecurringExpenseDetectionService recurringExpenseDetectionService;
  private final ActionFeedbackService actionFeedbackService;

  @Override
  public ActionRecommendation topRecommendation(String userId) {
    CashflowForecast forecast = forecastUseCase.forecast(userId);
    long minBalance = forecast.minimumBalance();
    PersonaProfileEntity persona = personaInferenceUseCase.getOrInfer(userId);
    String occupationCode = persona.getOccupationCode();

    List<ActionRecommendationEntity> all = recommendationRepository.findByActiveTrue();
    List<ActionRecommendation> candidates = new ArrayList<>();
    for (ActionRecommendationEntity rec : all) {
      if (!matchesBalance(rec, minBalance)) continue;
      if (!matchesOccupation(rec, occupationCode)) continue;
      double score =
          rec.getBaseScore() + actionFeedbackService.feedbackBoost(userId, rec.getActionTypeCode());
      if (rec.getOccupationCode() != null
          && rec.getOccupationCode().equalsIgnoreCase(occupationCode)) {
        score += 10.0;
      }
      candidates.add(
          new ActionRecommendation(
              rec.getActionTypeCode(),
              rec.getTitleTemplate(),
              easyReadPort.toEasyRead(rec.getDescriptionTemplate()),
              rec.getEstimatedImprovement(),
              score));
    }

    ActionRecommendation selected =
        banditScorer.chooseTop(candidates, ActionRecommendation::priorityScore);
    if (selected == null) {
      selected =
          new ActionRecommendation(
              "NO_ACTION",
              "현 상태 유지",
              easyReadPort.toEasyRead("현재 30일 위험 구간이 낮아 유지 전략을 권장합니다."),
              0,
              10);
    }
    log.info(
        LogMessages.Action.RECOMMENDATION_KPI,
        userId,
        selected.actionType(),
        selected.priorityScore(),
        occupationCode,
        candidates.size(),
        banditScorer.explorationEpsilon());
    return selected;
  }

  @Override
  public List<RecurringExpense> detectRecurringExpenses(String userId, String fintechUseNum) {
    return recurringExpenseDetectionService.detect(userId, fintechUseNum);
  }

  @Override
  public ActionExecutionResult executeRecommendedAction(String userId, String actionType) {
    return actionFeedbackService.execute(userId, actionType);
  }

  @Override
  public void submitRecommendationFeedback(String userId, String actionType, String feedback) {
    actionFeedbackService.submitFeedback(userId, actionType, feedback);
  }

  @Override
  public ActionFeedbackSummary getActionFeedbackSummary(String userId) {
    return actionFeedbackService.summarize(userId);
  }

  private boolean matchesBalance(ActionRecommendationEntity rec, long minBalance) {
    if (rec.getMinMinBalance() != null && minBalance < rec.getMinMinBalance()) return false;
    if (rec.getMaxMinBalance() != null && minBalance > rec.getMaxMinBalance()) return false;
    return true;
  }

  private boolean matchesOccupation(ActionRecommendationEntity rec, String occupationCode) {
    if (rec.getOccupationCode() == null) return true;
    return rec.getOccupationCode().equalsIgnoreCase(occupationCode);
  }
}
