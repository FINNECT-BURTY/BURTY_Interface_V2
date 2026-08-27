/**
 *
 *
 * <pre>
 * <b>Description  : 현금흐름 API 컨트롤러 (FinanceCashflowController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.cashflow
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
package com.burty.adapter.in.web.cashflow;

import com.burty.adapter.in.web.mapper.WebResponseMapper;
import com.burty.application.dto.action.ActionExecuteRequest;
import com.burty.application.dto.action.ActionExecutionResponse;
import com.burty.application.dto.action.ActionFeedbackRequest;
import com.burty.application.dto.action.ActionFeedbackSummaryResponse;
import com.burty.application.dto.action.ActionRecommendationResponse;
import com.burty.application.dto.cashflow.CashflowCriteriaResponse;
import com.burty.application.dto.cashflow.CashflowCriteriaUpdateRequest;
import com.burty.application.dto.cashflow.CashflowCriteriaUpdateResponse;
import com.burty.application.dto.cashflow.CashflowForecastResponse;
import com.burty.application.dto.cashflow.CashflowWhatIfRequest;
import com.burty.application.dto.cashflow.CashflowWhatIfResponse;
import com.burty.application.dto.cashflow.RecurringExpenseResponse;
import com.burty.application.dto.cashflow.RiskAssessmentResponse;
import com.burty.application.dto.shared.FlagResultResponse;
import com.burty.application.port.in.action.ActionRecommendationUseCase;
import com.burty.application.port.in.cashflow.CashflowForecastUseCase;
import com.burty.application.port.in.cashflow.RiskAssessmentUseCase;
import com.burty.core.annotation.CurrentUserId;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "BURTY Cashflow", description = "현금흐름 예측 및 행동 추천 API")
public class FinanceCashflowController extends BaseController {

  private final CashflowForecastUseCase cashflowForecastUseCase;
  private final RiskAssessmentUseCase riskAssessmentUseCase;
  private final ActionRecommendationUseCase actionRecommendationUseCase;
  private final WebResponseMapper webResponseMapper;

  @PostMapping("/cashflow/criteria")
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "현금흐름 커스텀 기준 저장", description = "안전잔액, 직접 입력 현재잔액, 월 변동지출 예산을 저장합니다.")
  public ApiResponse<CashflowCriteriaUpdateResponse> updateCashflowCriteria(
      @CurrentUserId String userId, @Valid @RequestBody CashflowCriteriaUpdateRequest request) {
    cashflowForecastUseCase.updateCashflowCriteria(
        userId,
        request.safetyBalance(),
        request.openingBalanceOverride(),
        request.monthlyVariableBudget());
    return ApiResponse.ok(
        new CashflowCriteriaUpdateResponse(
            true,
            webResponseMapper.toResponse(cashflowForecastUseCase.getCashflowCriteria(userId))));
  }

  @GetMapping("/cashflow/criteria")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "현금흐름 커스텀 기준 조회", description = "현금흐름 예측에 적용되는 사용자 직접 입력 기준을 조회합니다.")
  public ApiResponse<CashflowCriteriaResponse> cashflowCriteria(@CurrentUserId String userId) {
    return ApiResponse.ok(
        webResponseMapper.toResponse(cashflowForecastUseCase.getCashflowCriteria(userId)));
  }

  @GetMapping("/cashflow/forecast")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "30일 현금흐름 예측")
  public ApiResponse<CashflowForecastResponse> cashflowForecast(@CurrentUserId String userId) {
    return ApiResponse.ok(webResponseMapper.toResponse(cashflowForecastUseCase.forecast(userId)));
  }

  @PostMapping("/cashflow/what-if")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "What-if 현금흐름 시뮬레이션", description = "추가 지출·소득 변화 시나리오를 비교합니다.")
  public ApiResponse<CashflowWhatIfResponse> cashflowWhatIf(
      @CurrentUserId String userId, @Valid @RequestBody CashflowWhatIfRequest request) {
    var scenario =
        cashflowForecastUseCase.simulateWhatIf(
            userId,
            request.scenarioName(),
            request.extraDailyExpense(),
            request.incomeDelta(),
            request.expensePostponeDays());
    return ApiResponse.ok(CashflowWhatIfResponse.from(scenario, webResponseMapper::toResponse));
  }

  @GetMapping("/cashflow/risk")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "현금흐름 위험 진단")
  public ApiResponse<RiskAssessmentResponse> cashflowRisk(@CurrentUserId String userId) {
    return ApiResponse.ok(webResponseMapper.toResponse(riskAssessmentUseCase.assess(userId)));
  }

  @GetMapping("/cashflow/action")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "우선 행동 제안")
  public ApiResponse<ActionRecommendationResponse> cashflowAction(@CurrentUserId String userId) {
    return ApiResponse.ok(
        webResponseMapper.toResponse(actionRecommendationUseCase.topRecommendation(userId)));
  }

  @GetMapping("/cashflow/recurring-expenses")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "고정지출 자동 인식")
  public ApiResponse<List<RecurringExpenseResponse>> recurringExpenses(
      @CurrentUserId String userId, @RequestParam String fintechUseNum) {
    return ApiResponse.ok(
        webResponseMapper.toRecurringExpenseResponses(
            actionRecommendationUseCase.detectRecurringExpenses(userId, fintechUseNum)));
  }

  @PostMapping("/cashflow/action/execute")
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "추천 행동 실행")
  public ApiResponse<ActionExecutionResponse> executeAction(
      @CurrentUserId String userId, @Valid @RequestBody ActionExecuteRequest request) {
    return ApiResponse.ok(
        webResponseMapper.toResponse(
            actionRecommendationUseCase.executeRecommendedAction(userId, request.actionType())));
  }

  @PostMapping("/cashflow/action/feedback")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "추천 행동 피드백")
  public ApiResponse<FlagResultResponse> actionFeedback(
      @CurrentUserId String userId, @Valid @RequestBody ActionFeedbackRequest request) {
    actionRecommendationUseCase.submitRecommendationFeedback(
        userId, request.actionType(), request.feedback());
    return ApiResponse.ok(FlagResultResponse.of("saved", true));
  }

  @GetMapping("/cashflow/action/feedback-summary")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "행동 피드백 요약")
  public ApiResponse<ActionFeedbackSummaryResponse> actionFeedbackSummary(
      @CurrentUserId String userId) {
    return ApiResponse.ok(
        webResponseMapper.toResponse(actionRecommendationUseCase.getActionFeedbackSummary(userId)));
  }
}
