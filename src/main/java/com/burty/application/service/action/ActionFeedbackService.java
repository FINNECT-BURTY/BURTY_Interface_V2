/**
 *
 *
 * <pre>
 * <b>Description  : 행동추천 애플리케이션 서비스 (ActionFeedbackService)</b>
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

import com.burty.application.port.in.cashflow.RiskAssessmentUseCase;
import com.burty.application.port.in.user.PersonaInferenceUseCase;
import com.burty.application.service.support.AuditLogger;
import com.burty.core.constant.LogMessages;
import com.burty.domain.action.entity.ActionExecutionEntity;
import com.burty.domain.action.entity.ActionFeedbackEntity;
import com.burty.domain.action.entity.ActionFeedbackScoreEntity;
import com.burty.domain.action.model.ActionExecutionResult;
import com.burty.domain.action.model.ActionFeedbackSummary;
import com.burty.domain.action.repository.ActionExecutionRepository;
import com.burty.domain.action.repository.ActionFeedbackRepository;
import com.burty.domain.action.repository.ActionFeedbackScoreRepository;
import com.burty.domain.cashflow.model.RiskAssessment;
import com.burty.domain.user.entity.PersonaProfileEntity;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActionFeedbackService {

  private static final Logger log = LoggerFactory.getLogger(ActionFeedbackService.class);

  private final ActionExecutionRepository actionExecutionRepository;
  private final ActionFeedbackRepository actionFeedbackRepository;
  private final ActionFeedbackScoreRepository feedbackScoreRepository;
  private final PersonaInferenceUseCase personaInferenceUseCase;
  private final RiskAssessmentUseCase riskAssessmentUseCase;
  private final AuditLogger auditLogger;

  public ActionExecutionResult execute(String userId, String actionType) {
    String message =
        switch (actionType) {
          case "FOOD_BUDGET_CUT" -> "식비 예산 자동 조정이 적용되었습니다.";
          case "CARD_DUE_DATE_CHANGE" -> "카드 결제일 변경 신청 화면으로 연결했습니다.";
          case "DEBT_PRIORITY_CHANGE" ->
              "고금리 부채부터 확인할 수 있도록 참고 순서를 보여줍니다. 실제 상환 조건 변경은 금융기관 확인이 필요합니다.";
          case "EMERGENCY_POLICY_CHECK" -> "긴급 생활지원 정책 목록을 우선 노출했습니다.";
          default -> "행동 실행 요청이 접수되었습니다.";
        };
    ActionExecutionEntity executionEntity = new ActionExecutionEntity();
    executionEntity.setUserId(userId);
    executionEntity.setActionType(actionType);
    executionEntity.setExecuted(true);
    executionEntity.setMessage(message);
    executionEntity.setExecutedAt(LocalDateTime.now());
    actionExecutionRepository.save(executionEntity);
    auditLogger.logSuccess(
        userId, "EXECUTE_ACTION", actionType, buildExecutionAuditDetail(userId, actionType));
    return new ActionExecutionResult(userId, actionType, true, message);
  }

  public void submitFeedback(String userId, String actionType, String feedback) {
    boolean accept = "accept".equalsIgnoreCase(feedback);
    boolean reject = "reject".equalsIgnoreCase(feedback);

    ActionFeedbackScoreEntity score =
        feedbackScoreRepository
            .findByUserIdAndActionTypeCode(userId, actionType)
            .orElseGet(
                () -> {
                  ActionFeedbackScoreEntity created = new ActionFeedbackScoreEntity();
                  created.setUserId(userId);
                  created.setActionTypeCode(actionType);
                  return created;
                });
    if (accept) score.setAcceptCount(score.getAcceptCount() + 1);
    if (reject) score.setRejectCount(score.getRejectCount() + 1);
    score.setScore(score.getAcceptCount() - score.getRejectCount());
    feedbackScoreRepository.save(score);

    ActionFeedbackEntity feedbackEntity = new ActionFeedbackEntity();
    feedbackEntity.setUserId(userId);
    feedbackEntity.setActionType(actionType);
    feedbackEntity.setFeedback(feedback);
    feedbackEntity.setCreatedAt(LocalDateTime.now());
    actionFeedbackRepository.save(feedbackEntity);
    auditLogger.logSuccess(userId, "ACTION_FEEDBACK", actionType, "feedback=" + feedback);
    log.info(LogMessages.Action.FEEDBACK_KPI, userId, actionType, feedback, score.getScore());
  }

  public ActionFeedbackSummary summarize(String userId) {
    List<String> recent =
        actionExecutionRepository.findTop5ByUserIdOrderByExecutedAtDesc(userId).stream()
            .map(ActionExecutionEntity::getActionType)
            .toList();
    long executedCount = actionExecutionRepository.countByUserId(userId);
    long accepted = actionFeedbackRepository.countByUserIdAndFeedbackIgnoreCase(userId, "accept");
    long rejected = actionFeedbackRepository.countByUserIdAndFeedbackIgnoreCase(userId, "reject");
    return new ActionFeedbackSummary(
        userId, (int) executedCount, (int) accepted, (int) rejected, recent);
  }

  public int feedbackBoost(String userId, String actionTypeCode) {
    return feedbackScoreRepository
        .findByUserIdAndActionTypeCode(userId, actionTypeCode)
        .map(ActionFeedbackScoreEntity::getScore)
        .map(s -> Math.max(-20, Math.min(20, s * 2)))
        .orElse(0);
  }

  private String buildExecutionAuditDetail(String userId, String actionType) {
    StringBuilder sb = new StringBuilder("actionType=").append(actionType);
    try {
      PersonaProfileEntity persona = personaInferenceUseCase.getOrInfer(userId);
      if (persona != null) {
        if (persona.getOccupationCode() != null) {
          sb.append(",occupation=").append(persona.getOccupationCode());
        }
        if (persona.getResidenceCode() != null) {
          sb.append(",residence=").append(persona.getResidenceCode());
        }
      }
      RiskAssessment risk = riskAssessmentUseCase.assess(userId);
      if (risk != null) {
        sb.append(",riskLevel=").append(risk.level());
        sb.append(",projectedBalance=").append(risk.projectedBalance());
        if (risk.riskDate() != null) sb.append(",riskDate=").append(risk.riskDate());
      }
    } catch (Exception e) {
      log.debug("audit context enrichment skipped userId={} err={}", userId, e.getMessage());
    }
    return sb.toString();
  }
}
