package com.burty.adapter.in.web;

import com.burty.core.dto.response.ApiResponse;
import com.burty.core.controller.BaseController;
import com.burty.adapter.in.web.dto.*;
import com.burty.application.port.in.ExternalFinanceUseCase;
import com.burty.application.port.in.MyDataAuthUseCase;
import com.burty.application.port.in.BurtyUseCase;
import com.burty.application.port.in.WebAuthnUseCase;
import com.burty.application.port.in.AiAdvisoryUseCase;
import com.burty.domain.model.*;
import com.burty.security.AuthLevel;
import com.burty.security.RiskProofService;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.burty.adapter.out.alert.FamilyAlertSseBroker;

@RestController
@Tag(name = "BURTY Finance API", description = "BURTY 금융 상담/보안/가족보호/외부금융 연동 API")
public class BurtyController extends BaseController {
    private final BurtyUseCase moneyAgentUseCase;
    private final WebAuthnUseCase webAuthnUseCase;
    private final MyDataAuthUseCase myDataAuthUseCase;
    private final ExternalFinanceUseCase externalFinanceUseCase;
    private final AiAdvisoryUseCase aiAdvisoryUseCase;
    private final RiskProofService riskProofService;
    private final FamilyAlertSseBroker familyAlertSseBroker;

    public BurtyController(BurtyUseCase moneyAgentUseCase, WebAuthnUseCase webAuthnUseCase,
                                MyDataAuthUseCase myDataAuthUseCase, ExternalFinanceUseCase externalFinanceUseCase,
                                RiskProofService riskProofService, FamilyAlertSseBroker familyAlertSseBroker,
                                AiAdvisoryUseCase aiAdvisoryUseCase) {
        this.moneyAgentUseCase = moneyAgentUseCase;
        this.webAuthnUseCase = webAuthnUseCase;
        this.myDataAuthUseCase = myDataAuthUseCase;
        this.externalFinanceUseCase = externalFinanceUseCase;
        this.riskProofService = riskProofService;
        this.familyAlertSseBroker = familyAlertSseBroker;
        this.aiAdvisoryUseCase = aiAdvisoryUseCase;
    }

    @PostMapping("/consult")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<ConsultResponse> consult(@RequestBody ConsultRequest request) {
        ConsultationResult result = moneyAgentUseCase.consult(request.getUserId(), request.getQuestion());
        return ApiResponse.ok(new ConsultResponse(result.getSummary(), result.getSignalColor(), result.getRecommendedActions()));
    }

    @PostMapping("/ai/consult")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "AI 상담", description = "OpenAI 기반 자산 상담 결과를 쉬운 말로 제공합니다.")
    public ApiResponse<ConsultResponse> aiConsult(@RequestBody ConsultRequest request) {
        ConsultationResult result = aiAdvisoryUseCase.consultWithAi(request.getUserId(), request.getQuestion());
        return ApiResponse.ok(new ConsultResponse(result.getSummary(), result.getSignalColor(), result.getRecommendedActions()));
    }

    @GetMapping("/reports/monthly")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<MonthlyReportResponse> monthlyReport(@RequestParam String userId) {
        MonthlyReport report = moneyAgentUseCase.createMonthlyReport(userId);
        return ApiResponse.ok(new MonthlyReportResponse(report.getUserId(), report.getMonth(), report.getEasyReadSummary(),
                report.getSignalColor(), report.getPrimaryAction(), report.getKeyPoints()));
    }

    @PostMapping("/settings/limits")
    @AuthLevel(RiskLevel.LEVEL_2)
    public ApiResponse<Map<String, Object>> updateLimit(@RequestBody LimitUpdateRequest request) {
        moneyAgentUseCase.updateLimit(request.getUserId(), request.getLimit());
        return ApiResponse.ok(Map.of("updated", true, "limit", request.getLimit()));
    }

    @GetMapping("/settings/limits")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<Map<String, Object>> getLimit(@RequestParam String userId) {
        return ApiResponse.ok(Map.of("userId", userId, "limit", moneyAgentUseCase.getLimit(userId)));
    }

    @PostMapping("/security/level2/proof")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<Map<String, String>> issueLevel2Proof() {
        String userId = String.valueOf(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return ApiResponse.ok(Map.of("riskProof", riskProofService.issue(userId, RiskLevel.LEVEL_2)));
    }

    @PostMapping("/transfers")
    @AuthLevel(RiskLevel.LEVEL_3)
    public ApiResponse<TransferResponse> transfer(@RequestBody TransferRequest request) {
        TransferResult result = moneyAgentUseCase.transfer(
                request.getUserId(), request.getFromAccount(), request.getToAccount(), request.getAmount(),
                request.getDescription(), request.getAssertionToken()
        );
        return ApiResponse.ok(new TransferResponse(result.getTransferId(), result.getStatus(), result.isFamilyNotified()));
    }

    @GetMapping("/transfers/{transferId}")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<Map<String, Object>> getTransfer(@PathVariable String transferId) {
        TransferResult result = moneyAgentUseCase.getTransfer(transferId);
        if (result == null) {
            return ApiResponse.ok(Map.of("found", false, "transferId", transferId));
        }
        return ApiResponse.ok(Map.of(
                "found", true,
                "transferId", result.getTransferId(),
                "status", result.getStatus(),
                "familyNotified", result.isFamilyNotified()
        ));
    }

    @GetMapping("/transfers")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<List<TransferResponse>> transfers(@RequestParam String userId) {
        List<TransferResponse> responses = moneyAgentUseCase.getTransfers(userId).stream()
                .map(it -> new TransferResponse(it.getTransferId(), it.getStatus(), it.isFamilyNotified()))
                .toList();
        return ApiResponse.ok(responses);
    }

    @GetMapping("/family-alerts")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<List<FamilyAlertResponse>> familyAlerts(@RequestParam String userId) {
        List<FamilyAlertResponse> responses = moneyAgentUseCase.getFamilyAlerts(userId).stream()
                .map(it -> new FamilyAlertResponse(it.getUserId(), it.getMessage(), it.getSentAt()))
                .toList();
        return ApiResponse.ok(responses);
    }

    @GetMapping("/family-alerts/stream")
    @AuthLevel(RiskLevel.LEVEL_1)
    public SseEmitter familyAlertStream(@RequestParam String userId) {
        return familyAlertSseBroker.subscribe(userId);
    }

    @PostMapping("/family/consents")
    @AuthLevel(RiskLevel.LEVEL_2)
    public ApiResponse<Map<String, Object>> registerFamilyConsent(@RequestBody FamilyConsentRequest request) {
        moneyAgentUseCase.registerFamilyConsent(request.getParentUserId(), request.getChildUserId());
        return ApiResponse.ok(Map.of("registered", true));
    }

    @PatchMapping("/family/consents")
    @AuthLevel(RiskLevel.LEVEL_2)
    public ApiResponse<Map<String, Object>> updateFamilyConsent(@RequestBody FamilyConsentUpdateRequest request) {
        boolean updated = moneyAgentUseCase.updateFamilyConsent(
                request.getParentUserId(),
                request.getChildUserId(),
                request.isConsented()
        );
        return ApiResponse.ok(Map.of("updated", updated));
    }

    @DeleteMapping("/family/consents")
    @AuthLevel(RiskLevel.LEVEL_2)
    public ApiResponse<Map<String, Object>> revokeFamilyConsent(
            @RequestParam String parentUserId,
            @RequestParam String childUserId
    ) {
        boolean deleted = moneyAgentUseCase.revokeFamilyConsent(parentUserId, childUserId);
        return ApiResponse.ok(Map.of("deleted", deleted));
    }

    @GetMapping("/family/consents")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<List<FamilyConsentResponse>> familyConsents(@RequestParam String parentUserId) {
        List<FamilyConsentResponse> responses = moneyAgentUseCase.getFamilyConsents(parentUserId).stream()
                .map(it -> new FamilyConsentResponse(it.getParentUserId(), it.getChildUserId(), it.isConsented()))
                .toList();
        return ApiResponse.ok(responses);
    }

    @GetMapping("/family/dashboard")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<FamilyDashboardResponse> familyDashboard(@RequestParam String userId) {
        FamilyDashboardSummary summary = moneyAgentUseCase.getFamilyDashboardSummary(userId);
        return ApiResponse.ok(new FamilyDashboardResponse(
                summary.getUserId(),
                summary.getAlertCount(),
                summary.getUnusualTransactionCount(),
                summary.getMonthlyReportDeliveredCount()
        ));
    }

    @PostMapping("/security/webauthn/register/begin")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<ChallengeResponse> beginRegistration(@RequestBody WebAuthnBeginRequest request) {
        String challengeId = webAuthnUseCase.beginRegistration(request.getUserId());
        return ApiResponse.ok(new ChallengeResponse(challengeId));
    }

    @PostMapping("/security/webauthn/register/finish")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<BiometricAuthResponse> finishRegistration(@RequestBody WebAuthnFinishRequest request) {
        BiometricAuthResult result = webAuthnUseCase.finishRegistration(
                request.getUserId(),
                request.getChallengeId(),
                request.getPayload(),
                request.getDeviceFingerprint(),
                request.getPlatform(),
                request.getBiometricType()
        );
        return ApiResponse.ok(new BiometricAuthResponse(
                result.getUserId(),
                result.getDeviceId(),
                result.getDeviceToken(),
                result.getAccessToken(),
                result.isAuthenticated() ? riskProofService.issue(request.getUserId(), RiskLevel.LEVEL_3) : null,
                result.isAuthenticated(),
                result.isTrustedDevice()
        ));
    }

    @PostMapping("/security/webauthn/authenticate/begin")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<ChallengeResponse> beginAuthentication(@RequestBody WebAuthnBeginRequest request) {
        String challengeId = webAuthnUseCase.beginAuthentication(request.getUserId());
        return ApiResponse.ok(new ChallengeResponse(challengeId));
    }

    @PostMapping("/security/webauthn/authenticate/finish")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<BiometricAuthResponse> finishAuthentication(@RequestBody WebAuthnFinishRequest request) {
        BiometricAuthResult result = webAuthnUseCase.finishAuthentication(
                request.getUserId(),
                request.getChallengeId(),
                request.getPayload(),
                request.getDeviceToken()
        );
        return ApiResponse.ok(new BiometricAuthResponse(
                result.getUserId(),
                result.getDeviceId(),
                null,
                result.getAccessToken(),
                result.isAuthenticated() ? riskProofService.issue(request.getUserId(), RiskLevel.LEVEL_3) : null,
                result.isAuthenticated(),
                result.isTrustedDevice()
        ));
    }

    @GetMapping("/mydata/oauth/authorize")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<AuthorizeUrlResponse> authorizeMyData(@RequestParam String userId) {
        String authorizeUrl = myDataAuthUseCase.createAuthorizeUrl(userId);
        return ApiResponse.ok(new AuthorizeUrlResponse(authorizeUrl));
    }

    @PostMapping("/mydata/oauth/callback")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<Map<String, Object>> myDataCallback(@RequestBody MyDataCallbackRequest request) {
        boolean linked = myDataAuthUseCase.exchangeAuthorizationCode(request.getUserId(), request.getCode());
        return ApiResponse.ok(Map.of("linked", linked));
    }

    @DeleteMapping("/mydata/links/{institutionCode}")
    @AuthLevel(RiskLevel.LEVEL_2)
    public ApiResponse<Map<String, Object>> unlinkMyDataInstitution(@RequestParam String userId, @PathVariable String institutionCode
    ) {
        boolean unlinked = moneyAgentUseCase.unlinkMyDataInstitution(userId, institutionCode);
        return ApiResponse.ok(Map.of("unlinked", unlinked, "institutionCode", institutionCode));
    }

    @GetMapping("/assets/summary")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<Map<String, Object>> assetSummary(@RequestParam String userId) {
        return ApiResponse.ok(moneyAgentUseCase.getAssetSummary(userId));
    }

    @GetMapping("/assets/trend")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<List<Map<String, Object>>> assetTrend(@RequestParam String userId) {
        return ApiResponse.ok(moneyAgentUseCase.getAssetTrend(userId));
    }

    @PostMapping("/cashflow/criteria")
    @AuthLevel(RiskLevel.LEVEL_2)
    @Operation(summary = "현금흐름 커스텀 기준 저장", description = "안전잔액, 직접 입력 현재잔액, 월 변동지출 예산을 저장합니다.")
    public ApiResponse<Map<String, Object>> updateCashflowCriteria(@RequestBody Map<String, Object> request) {
        String userId = String.valueOf(request.get("userId"));
        moneyAgentUseCase.updateCashflowCriteria(
                userId,
                nullableLong(request.get("safetyBalance")),
                nullableLong(request.get("openingBalanceOverride")),
                nullableLong(request.get("monthlyVariableBudget"))
        );
        return ApiResponse.ok(Map.of("updated", true, "criteria", moneyAgentUseCase.getCashflowCriteria(userId)));
    }

    @GetMapping("/cashflow/criteria")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "현금흐름 커스텀 기준 조회", description = "현금흐름 예측에 적용되는 사용자 직접 입력 기준을 조회합니다.")
    public ApiResponse<Map<String, Object>> cashflowCriteria(@RequestParam String userId) {
        return ApiResponse.ok(moneyAgentUseCase.getCashflowCriteria(userId));
    }

    @GetMapping("/cashflow/forecast")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "30일 현금흐름 예측", description = "사용자의 향후 30일 예상 잔액 흐름과 위험 시점을 조회합니다.")
    public ApiResponse<CashflowForecastResponse> cashflowForecast(@RequestParam String userId) {
        CashflowForecast forecast = moneyAgentUseCase.getCashflowForecast(userId);
        return ApiResponse.ok(new CashflowForecastResponse(
                forecast.getUserId(),
                forecast.getGeneratedDate(),
                forecast.getOpeningBalance(),
                forecast.getMinimumBalance(),
                forecast.getRiskDate(),
                forecast.getRiskReason(),
                forecast.getDailyBalances(),
                forecast.getSafetyBalance(),
                forecast.getDataSource(),
                forecast.isCustomCriteriaUsed()
        ));
    }

    @GetMapping("/cashflow/risk")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "현금흐름 위험 진단", description = "예측 결과를 기준으로 위험 단계를 진단합니다.")
    public ApiResponse<RiskAssessmentResponse> cashflowRisk(@RequestParam String userId) {
        RiskAssessment risk = moneyAgentUseCase.getCashflowRisk(userId);
        return ApiResponse.ok(new RiskAssessmentResponse(
                risk.getUserId(),
                risk.getLevel(),
                risk.getThreshold(),
                risk.getReason(),
                risk.getRiskDate(),
                risk.getProjectedBalance()
        ));
    }

    @GetMapping("/cashflow/action")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "우선 행동 제안", description = "현재 사용자에게 가장 효과적인 Top-1 행동을 제안합니다.")
    public ApiResponse<ActionRecommendationResponse> cashflowAction(@RequestParam String userId) {
        ActionRecommendation action = moneyAgentUseCase.getTopActionRecommendation(userId);
        return ApiResponse.ok(new ActionRecommendationResponse(
                action.getActionType(),
                action.getTitle(),
                action.getDescription(),
                action.getEstimatedImprovement(),
                action.getPriorityScore(),
                action.getAdvisoryBoundary()
        ));
    }

    @GetMapping("/policy/match")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "청년 정책 매칭", description = "사용자 속성과 정책 조건을 매칭해 상위 정책을 추천합니다.")
    public ApiResponse<List<PolicyMatchResponse>> policyMatch(@RequestParam String userId) {
        List<PolicyMatchResponse> responses = moneyAgentUseCase.getPolicyMatches(userId).stream()
                .map(match -> new PolicyMatchResponse(
                        match.getPolicyId(),
                        match.getPolicyName(),
                        match.getSupportType(),
                        match.getReason(),
                        match.getPriorityScore()
                ))
                .toList();
        return ApiResponse.ok(responses);
    }

    @PostMapping("/policy/{policyCode}/apply")
    @AuthLevel(RiskLevel.LEVEL_2)
    @Operation(summary = "정책 신청 표시", description = "사용자가 정책을 신청했음을 기록합니다. 신청률 KPI에 반영됩니다.")
    public ApiResponse<Map<String, Object>> applyPolicy(@RequestParam String userId, @PathVariable String policyCode) {
        moneyAgentUseCase.applyPolicy(userId, policyCode);
        return ApiResponse.ok(Map.of("applied", true, "policyCode", policyCode, "userId", userId));
    }

    @GetMapping("/cashflow/recurring-expenses")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "고정지출 자동 인식", description = "거래내역을 분석해 반복 지출 항목을 식별합니다.")
    public ApiResponse<List<RecurringExpenseResponse>> recurringExpenses(@RequestParam String userId, @RequestParam String fintechUseNum) {
        List<RecurringExpenseResponse> responses = moneyAgentUseCase.detectRecurringExpenses(userId, fintechUseNum).stream()
                .map(item -> new RecurringExpenseResponse(item.getName(), item.getAmount(), item.getDayOfMonth()))
                .toList();
        return ApiResponse.ok(responses);
    }

    @PostMapping("/cashflow/action/execute")
    @AuthLevel(RiskLevel.LEVEL_2)
    @Operation(summary = "추천 행동 실행", description = "추천된 행동을 실행하고 결과를 반환합니다.")
    public ApiResponse<ActionExecutionResponse> executeAction(@RequestBody ActionExecuteRequest request) {
        ActionExecutionResult result = moneyAgentUseCase.executeRecommendedAction(request.getUserId(), request.getActionType());
        return ApiResponse.ok(new ActionExecutionResponse(
                result.getUserId(),
                result.getActionType(),
                result.isExecuted(),
                result.getMessage()
        ));
    }

    @PostMapping("/cashflow/action/feedback")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "추천 행동 피드백", description = "추천 행동에 대한 수락/거절 피드백을 저장합니다.")
    public ApiResponse<Map<String, Object>> actionFeedback(@RequestBody ActionFeedbackRequest request) {
        moneyAgentUseCase.submitRecommendationFeedback(request.getUserId(), request.getActionType(), request.getFeedback());
        return ApiResponse.ok(Map.of("saved", true));
    }

    @GetMapping("/cashflow/action/feedback-summary")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "행동 피드백 요약", description = "행동 실행/수락/거절 현황을 요약해 반환합니다.")
    public ApiResponse<ActionFeedbackSummaryResponse> actionFeedbackSummary(@RequestParam String userId) {
        ActionFeedbackSummary summary = moneyAgentUseCase.getActionFeedbackSummary(userId);
        return ApiResponse.ok(new ActionFeedbackSummaryResponse(
                summary.getUserId(),
                summary.getTotalExecutedActions(),
                summary.getAcceptedCount(),
                summary.getRejectedCount(),
                summary.getRecentExecutedActions()
        ));
    }

    @PostMapping("/external/kakao-bank/transfer")
    @AuthLevel(RiskLevel.LEVEL_3)
    @Operation(summary = "카카오뱅크 이체", description = "레벨3 인증 이후 카카오뱅크로 송금을 요청합니다.")
    public ApiResponse<Map<String, Object>> kakaoBankTransfer(@RequestBody TransferRequest request) {
        return ApiResponse.ok(externalFinanceUseCase.transferToKakaoBank(request.getUserId(), request.getToAccount(), request.getAmount()));
    }

    @PostMapping("/external/hana-bank/transfer")
    @AuthLevel(RiskLevel.LEVEL_3)
    @Operation(summary = "하나은행 이체", description = "레벨3 인증 이후 하나은행으로 송금을 요청합니다.")
    public ApiResponse<Map<String, Object>> hanaBankTransfer(@RequestBody TransferRequest request) {
        return ApiResponse.ok(externalFinanceUseCase.transferToHanaBank(request.getUserId(), request.getToAccount(), request.getAmount()));
    }

    @PostMapping("/external/kb-bank/transfer")
    @AuthLevel(RiskLevel.LEVEL_3)
    @Operation(summary = "KB국민은행 이체", description = "레벨3 인증 이후 KB국민은행으로 송금을 요청합니다.")
    public ApiResponse<Map<String, Object>> kbBankTransfer(@RequestBody TransferRequest request) {
        return ApiResponse.ok(externalFinanceUseCase.transferToKbBank(request.getUserId(), request.getToAccount(), request.getAmount()));
    }

    @PostMapping("/external/shinhan-bank/transfer")
    @AuthLevel(RiskLevel.LEVEL_3)
    @Operation(summary = "신한은행 이체", description = "레벨3 인증 이후 신한은행으로 송금을 요청합니다.")
    public ApiResponse<Map<String, Object>> shinhanBankTransfer(@RequestBody TransferRequest request) {
        return ApiResponse.ok(externalFinanceUseCase.transferToShinhanBank(request.getUserId(), request.getToAccount(), request.getAmount()));
    }

    @PostMapping("/external/im-bank/transfer")
    @AuthLevel(RiskLevel.LEVEL_3)
    @Operation(summary = "iM뱅크 이체", description = "레벨3 인증 이후 iM뱅크로 송금을 요청합니다.")
    public ApiResponse<Map<String, Object>> imBankTransfer(@RequestBody TransferRequest request) {
        return ApiResponse.ok(externalFinanceUseCase.transferToImBank(request.getUserId(), request.getToAccount(), request.getAmount()));
    }

    @GetMapping("/external/openbanking/accounts")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "오픈뱅킹 계좌 조회", description = "사용자의 오픈뱅킹 연계 계좌 목록을 조회합니다.")
    public ApiResponse<Map<String, Object>> openBankingAccounts(@RequestParam String userId) {
        return ApiResponse.ok(externalFinanceUseCase.getOpenBankingAccounts(userId));
    }

    @GetMapping("/external/openbanking/balance")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "오픈뱅킹 잔액 조회", description = "fintechUseNum 기준 계좌 잔액을 조회합니다.")
    public ApiResponse<Map<String, Object>> openBankingBalance(@RequestParam String userId, @RequestParam String fintechUseNum
    ) {
        return ApiResponse.ok(externalFinanceUseCase.getOpenBankingBalance(userId, fintechUseNum));
    }

    @GetMapping("/external/openbanking/transactions")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "오픈뱅킹 거래내역 조회", description = "fintechUseNum 기준 거래내역을 조회합니다.")
    public ApiResponse<Map<String, Object>> openBankingTransactions(@RequestParam String userId, @RequestParam String fintechUseNum
    ) {
        return ApiResponse.ok(externalFinanceUseCase.getOpenBankingTransactions(userId, fintechUseNum));
    }

    @PostMapping("/external/openbanking/transfer")
    @AuthLevel(RiskLevel.LEVEL_3)
    @Operation(summary = "오픈뱅킹 이체", description = "레벨3 인증 이후 오픈뱅킹 이체를 요청합니다.")
    public ApiResponse<Map<String, Object>> openBankingTransfer(@RequestBody TransferRequest request) {
        return ApiResponse.ok(externalFinanceUseCase.transferByOpenBanking(
                request.getUserId(),
                request.getFromAccount(),
                request.getToAccount(),
                request.getAmount(),
                request.getIdempotencyKey()
        ));
    }

    @GetMapping("/external/pension/summary")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<Map<String, Object>> pensionSummary(@RequestParam String userId) {
        return ApiResponse.ok(externalFinanceUseCase.getPensionSummary(userId));
    }

    private Long nullableLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
