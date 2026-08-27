package com.burty.adapter.in.web.finance;

import com.burty.application.dto.finance.OpenBankingAuthorizeResponse;
import com.burty.application.dto.shared.FlagResultResponse;
import com.burty.application.port.in.finance.ExternalFinanceUseCase;
import com.burty.core.annotation.CurrentUserId;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/external/openbanking/oauth")
@RequiredArgsConstructor
@Tag(name = "BURTY OpenBanking OAuth", description = "오픈뱅킹 사용자 동의 OAuth API")
public class OpenBankingOAuthController extends BaseController {

  private final ExternalFinanceUseCase externalFinanceUseCase;

  @GetMapping("/authorize")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "오픈뱅킹 인가 URL")
  public ApiResponse<OpenBankingAuthorizeResponse> authorize(@CurrentUserId String userId) {
    return ApiResponse.ok(externalFinanceUseCase.createOpenBankingAuthorize(userId));
  }

  @GetMapping("/callback")
  @Operation(summary = "오픈뱅킹 OAuth 콜백 (redirect)")
  public ApiResponse<FlagResultResponse> callback(
      @RequestParam String code, @RequestParam String state) {
    boolean ok = externalFinanceUseCase.exchangeOpenBankingAuthorizationCodeByState(state, code);
    return ApiResponse.ok(FlagResultResponse.of("linked", ok));
  }

  @PostMapping("/callback")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "오픈뱅킹 OAuth 콜백 (API)")
  public ApiResponse<FlagResultResponse> callbackPost(
      @CurrentUserId String userId, @RequestParam String code) {
    boolean ok = externalFinanceUseCase.exchangeOpenBankingAuthorizationCode(userId, code);
    return ApiResponse.ok(FlagResultResponse.of("linked", ok));
  }
}
