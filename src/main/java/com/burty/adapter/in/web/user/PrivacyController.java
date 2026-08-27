package com.burty.adapter.in.web.user;

import com.burty.application.service.user.DataSubjectRequestService;
import com.burty.application.service.user.UserWithdrawalService;
import com.burty.core.annotation.CurrentUserId;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.domain.user.entity.DataErasureRequestEntity;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 정보주체 권리 행사 API (열람·정정·파기).
 *
 * <p>개인정보보호법상 정보주체가 행사할 수 있는 권리를 API 로 제공한다. 마이데이터 사업자 등록을 목표로 한다면 필수 창구이고, 기존 코드베이스에는 이 개념 자체가
 * 없었다.
 */
@RestController
@RequestMapping("/privacy")
@Tag(name = "BURTY Privacy", description = "개인정보 열람·정정·파기 (정보주체 권리 행사) API")
@RequiredArgsConstructor
public class PrivacyController extends BaseController {

  private final DataSubjectRequestService dataSubjectRequestService;
  private final UserWithdrawalService userWithdrawalService;

  /** 정정 요청. 본인확인 기반 항목(전화번호·생년월일)은 별도 절차가 필요하므로 여기서 다루지 않는다. */
  public record RectifyRequest(
      @NotBlank(message = "정정할 항목명이 필요합니다") @Size(max = 40) String field,
      @NotBlank(message = "정정할 값이 필요합니다") @Size(max = 200) String value) {}

  public record WithdrawRequest(@Size(max = 200) String reason) {}

  public record WithdrawResponse(String status, String retentionUntil, String note) {}

  @GetMapping("/me/export")
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "개인정보 열람", description = "서비스가 보유 중인 내 개인정보 전체를 구조화해 반환합니다.")
  public ApiResponse<Map<String, Object>> export(@CurrentUserId String userId) {
    return ApiResponse.ok(dataSubjectRequestService.exportPersonalData(userId));
  }

  @PatchMapping("/me")
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "개인정보 정정", description = "정정 가능한 항목을 수정합니다.")
  public ApiResponse<Boolean> rectify(
      @CurrentUserId String userId, @Valid @RequestBody RectifyRequest request) {
    dataSubjectRequestService.rectify(userId, request.field(), request.value());
    return ApiResponse.ok(true);
  }

  /**
   * 회원 탈퇴.
   *
   * <p>즉시 직접 식별정보를 익명화하고 인증 수단과 마이데이터 수집분을 파기한다. 전자금융거래 기록은 법정 보존의무에 따라 보존기간까지 유지된 뒤 자동 파기된다. 응답에 그
   * 시점을 명시한다.
   */
  @DeleteMapping("/me")
  @AuthLevel(RiskLevel.LEVEL_3)
  @Operation(summary = "회원 탈퇴 및 개인정보 파기")
  public ApiResponse<WithdrawResponse> withdraw(
      @CurrentUserId String userId, @Valid @RequestBody WithdrawRequest request) {
    DataErasureRequestEntity erasure =
        userWithdrawalService.withdraw(
            userId, request.reason() == null ? "USER_REQUEST" : request.reason());
    return ApiResponse.ok(
        new WithdrawResponse(
            erasure.getStatus().name(),
            String.valueOf(erasure.getRetentionUntil()),
            "직접 식별정보는 즉시 파기되었습니다. 전자금융거래 기록은 법정 보존기간 경과 후 자동 파기됩니다."));
  }
}
