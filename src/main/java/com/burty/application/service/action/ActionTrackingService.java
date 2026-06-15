/**
 *
 *
 * <pre>
 * <b>Description  : 행동추천 애플리케이션 서비스 (ActionTrackingService)</b>
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

import com.burty.application.dto.action.ActionTrackingResponse;
import com.burty.application.port.in.action.ActionTrackingUseCase;
import com.burty.application.port.in.cashflow.RiskAssessmentUseCase;
import com.burty.domain.action.repository.ActionExecutionRepository;
import com.burty.domain.action.repository.ActionFeedbackRepository;
import com.burty.domain.cashflow.model.RiskAssessment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActionTrackingService implements ActionTrackingUseCase {

  private final ActionExecutionRepository actionExecutionRepository;
  private final ActionFeedbackRepository actionFeedbackRepository;
  private final RiskAssessmentUseCase riskAssessmentUseCase;

  @Override
  public ActionTrackingResponse tracking(String userId, String actionType) {
    RiskAssessment risk = riskAssessmentUseCase.assess(userId);
    return new ActionTrackingResponse(
        userId,
        actionType,
        actionExecutionRepository.countByUserIdAndActionType(userId, actionType),
        actionFeedbackRepository.countByUserIdAndActionTypeAndFeedbackIgnoreCase(
            userId, actionType, "accept"),
        actionFeedbackRepository.countByUserIdAndActionTypeAndFeedbackIgnoreCase(
            userId, actionType, "reject"),
        risk.projectedBalance(),
        risk.level());
  }
}
