/**
 *
 *
 * <pre>
 * <b>Description  : 행동추천 유스케이스 포트 (ActionRecommendationUseCase)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.in.action
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
package com.burty.application.port.in.action;

import com.burty.domain.action.model.ActionExecutionResult;
import com.burty.domain.action.model.ActionFeedbackSummary;
import com.burty.domain.action.model.ActionRecommendation;
import com.burty.domain.cashflow.model.RecurringExpense;
import java.util.List;

public interface ActionRecommendationUseCase {

  ActionRecommendation topRecommendation(String userId);

  List<RecurringExpense> detectRecurringExpenses(String userId, String fintechUseNum);

  ActionExecutionResult executeRecommendedAction(String userId, String actionType);

  void submitRecommendationFeedback(String userId, String actionType, String feedback);

  ActionFeedbackSummary getActionFeedbackSummary(String userId);
}
