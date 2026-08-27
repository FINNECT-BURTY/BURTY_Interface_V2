/**
 *
 *
 * <pre>
 * <b>Description  : 금융 API 컨트롤러 (ExternalFinanceController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.finance
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
package com.burty.adapter.in.web.finance;

import com.burty.application.dto.finance.ExternalTransferResponse;
import com.burty.application.dto.finance.OpenBankingAccountsResponse;
import com.burty.application.dto.finance.OpenBankingBalanceResponse;
import com.burty.application.dto.finance.OpenBankingTransactionsResponse;
import com.burty.application.dto.finance.PensionSummaryResponse;
import com.burty.application.dto.finance.TransferRequest;
import com.burty.application.port.in.finance.ExternalFinanceUseCase;
import com.burty.core.annotation.CurrentUserId;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "BURTY External Finance", description = "외부 금융기관 연동 API")
public class ExternalFinanceController extends BaseController {

  private final ExternalFinanceUseCase externalFinanceUseCase;

  @PostMapping("/external/kakao-bank/transfer")
  @AuthLevel(RiskLevel.LEVEL_3)
  @Operation(summary = "카카오뱅크 이체")
  public ApiResponse<ExternalTransferResponse> kakaoBankTransfer(
      @CurrentUserId String userId, @Valid @RequestBody TransferRequest request) {
    return ApiResponse.ok(
        externalFinanceUseCase.transferToKakaoBank(userId, request.toAccount(), request.amount()));
  }

  @PostMapping("/external/hana-bank/transfer")
  @AuthLevel(RiskLevel.LEVEL_3)
  @Operation(summary = "하나은행 이체")
  public ApiResponse<ExternalTransferResponse> hanaBankTransfer(
      @CurrentUserId String userId, @Valid @RequestBody TransferRequest request) {
    return ApiResponse.ok(
        externalFinanceUseCase.transferToHanaBank(userId, request.toAccount(), request.amount()));
  }

  @PostMapping("/external/kb-bank/transfer")
  @AuthLevel(RiskLevel.LEVEL_3)
  @Operation(summary = "KB국민은행 이체")
  public ApiResponse<ExternalTransferResponse> kbBankTransfer(
      @CurrentUserId String userId, @Valid @RequestBody TransferRequest request) {
    return ApiResponse.ok(
        externalFinanceUseCase.transferToKbBank(userId, request.toAccount(), request.amount()));
  }

  @PostMapping("/external/shinhan-bank/transfer")
  @AuthLevel(RiskLevel.LEVEL_3)
  @Operation(summary = "신한은행 이체")
  public ApiResponse<ExternalTransferResponse> shinhanBankTransfer(
      @CurrentUserId String userId, @Valid @RequestBody TransferRequest request) {
    return ApiResponse.ok(
        externalFinanceUseCase.transferToShinhanBank(
            userId, request.toAccount(), request.amount()));
  }

  @PostMapping("/external/im-bank/transfer")
  @AuthLevel(RiskLevel.LEVEL_3)
  @Operation(summary = "iM뱅크 이체")
  public ApiResponse<ExternalTransferResponse> imBankTransfer(
      @CurrentUserId String userId, @Valid @RequestBody TransferRequest request) {
    return ApiResponse.ok(
        externalFinanceUseCase.transferToImBank(userId, request.toAccount(), request.amount()));
  }

  @GetMapping("/external/openbanking/accounts")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "오픈뱅킹 계좌 조회")
  public ApiResponse<OpenBankingAccountsResponse> openBankingAccounts(
      @CurrentUserId String userId) {
    return ApiResponse.ok(externalFinanceUseCase.getOpenBankingAccounts(userId));
  }

  @GetMapping("/external/openbanking/balance")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "오픈뱅킹 잔액 조회")
  public ApiResponse<OpenBankingBalanceResponse> openBankingBalance(
      @CurrentUserId String userId, @RequestParam String fintechUseNum) {
    return ApiResponse.ok(externalFinanceUseCase.getOpenBankingBalance(userId, fintechUseNum));
  }

  @GetMapping("/external/openbanking/transactions")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "오픈뱅킹 거래내역 조회")
  public ApiResponse<OpenBankingTransactionsResponse> openBankingTransactions(
      @CurrentUserId String userId, @RequestParam String fintechUseNum) {
    return ApiResponse.ok(externalFinanceUseCase.getOpenBankingTransactions(userId, fintechUseNum));
  }

  @PostMapping("/external/openbanking/transfer")
  @AuthLevel(RiskLevel.LEVEL_3)
  @Operation(summary = "오픈뱅킹 이체")
  public ApiResponse<ExternalTransferResponse> openBankingTransfer(
      @CurrentUserId String userId, @Valid @RequestBody TransferRequest request) {
    return ApiResponse.ok(
        externalFinanceUseCase.transferByOpenBanking(
            userId,
            request.fromAccount(),
            request.toAccount(),
            request.amount(),
            request.idempotencyKey()));
  }

  @GetMapping("/external/pension/summary")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<PensionSummaryResponse> pensionSummary(@CurrentUserId String userId) {
    return ApiResponse.ok(externalFinanceUseCase.getPensionSummary(userId));
  }
}
