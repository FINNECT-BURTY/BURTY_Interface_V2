package com.nuri.adapter.in.web;

import com.nuri.core.dto.response.ApiResponse;
import com.nuri.adapter.in.web.dto.*;
import com.nuri.application.port.in.ExternalFinanceUseCase;
import com.nuri.application.port.in.MyDataAuthUseCase;
import com.nuri.application.port.in.NuriUseCase;
import com.nuri.application.port.in.WebAuthnUseCase;
import com.nuri.application.port.in.AiAdvisoryUseCase;
import com.nuri.domain.model.*;
import com.nuri.security.AuthLevel;
import com.nuri.security.RiskProofService;
import com.nuri.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.nuri.adapter.out.alert.FamilyAlertSseBroker;

@RestController
@RequestMapping("/api/nuri")
@Tag(name = "NURI Finance API", description = "NURI 금융 상담/보안/가족보호/외부금융 연동 API")
public class NuriController {
    private final NuriUseCase moneyAgentUseCase;
    private final WebAuthnUseCase webAuthnUseCase;
    private final MyDataAuthUseCase myDataAuthUseCase;
    private final ExternalFinanceUseCase externalFinanceUseCase;
    private final AiAdvisoryUseCase aiAdvisoryUseCase;
    private final RiskProofService riskProofService;
    private final FamilyAlertSseBroker familyAlertSseBroker;

    public NuriController(NuriUseCase moneyAgentUseCase, WebAuthnUseCase webAuthnUseCase,
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
    public ApiResponse<Map<String, String>> beginRegistration(@RequestBody WebAuthnBeginRequest request) {
        String challengeId = webAuthnUseCase.beginRegistration(request.getUserId());
        return ApiResponse.ok(Map.of("challengeId", challengeId));
    }

    @PostMapping("/security/webauthn/register/finish")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<Map<String, Object>> finishRegistration(@RequestBody WebAuthnFinishRequest request) {
        boolean verified = webAuthnUseCase.finishRegistration(request.getUserId(), request.getChallengeId(), request.getPayload());
        return ApiResponse.ok(Map.of("registered", verified));
    }

    @PostMapping("/security/webauthn/authenticate/begin")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<Map<String, String>> beginAuthentication(@RequestBody WebAuthnBeginRequest request) {
        String challengeId = webAuthnUseCase.beginAuthentication(request.getUserId());
        return ApiResponse.ok(Map.of("challengeId", challengeId));
    }

    @PostMapping("/security/webauthn/authenticate/finish")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<Map<String, Object>> finishAuthentication(@RequestBody WebAuthnFinishRequest request) {
        boolean verified = webAuthnUseCase.finishAuthentication(request.getUserId(), request.getChallengeId(), request.getPayload());
        if (!verified) {
            return ApiResponse.ok(Map.of("authenticated", false));
        }
        return ApiResponse.ok(Map.of(
                "authenticated", true,
                "riskProof", riskProofService.issue(request.getUserId(), RiskLevel.LEVEL_3)
        ));
    }

    @GetMapping("/mydata/oauth/authorize")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<Map<String, String>> authorizeMyData(@RequestParam String userId) {
        String authorizeUrl = myDataAuthUseCase.createAuthorizeUrl(userId);
        return ApiResponse.ok(Map.of("authorizeUrl", authorizeUrl));
    }

    @PostMapping("/mydata/oauth/callback")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<Map<String, Object>> myDataCallback(@RequestBody MyDataCallbackRequest request) {
        boolean linked = myDataAuthUseCase.exchangeAuthorizationCode(request.getUserId(), request.getCode());
        return ApiResponse.ok(Map.of("linked", linked));
    }

    @DeleteMapping("/mydata/links/{institutionCode}")
    @AuthLevel(RiskLevel.LEVEL_2)
    public ApiResponse<Map<String, Object>> unlinkMyDataInstitution(
            @RequestParam String userId,
            @PathVariable String institutionCode
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

    @GetMapping("/external/pension/summary")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<Map<String, Object>> pensionSummary(@RequestParam String userId) {
        return ApiResponse.ok(externalFinanceUseCase.getPensionSummary(userId));
    }
}
