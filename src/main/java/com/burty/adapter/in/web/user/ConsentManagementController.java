/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 API 컨트롤러 (ConsentManagementController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.user
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
package com.burty.adapter.in.web.user;

import com.burty.application.dto.shared.SimpleResultResponse;
import com.burty.application.dto.user.ConsentResponse;
import com.burty.application.port.in.mydata.MyDataAuthUseCase;
import com.burty.application.port.in.user.ConsentManagementUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consents")
@RequiredArgsConstructor
@Tag(name = "BURTY Consent Management", description = "동의/연결 해제 관리 API")
public class ConsentManagementController extends BaseController {

  private final ConsentManagementUseCase consentManagementUseCase;
  private final MyDataAuthUseCase myDataAuthUseCase;

  @GetMapping
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "동의 이력 조회", description = "사용자의 개인정보/마이데이터/위치/보안 로그 동의 이력을 조회합니다.")
  public ApiResponse<List<ConsentResponse>> consents(@RequestParam String userId) {
    return ApiResponse.ok(consentManagementUseCase.listConsents(userId));
  }

  @PostMapping("/{consentId}/revoke")
  @AuthLevel(RiskLevel.LEVEL_3)
  @Operation(summary = "동의 철회", description = "동의 이력을 철회 처리합니다.")
  public ApiResponse<SimpleResultResponse> revokeConsent(
      @PathVariable String consentId, @RequestParam(required = false) String reason) {
    consentManagementUseCase.revokeConsent(consentId, reason);
    return ApiResponse.ok(new SimpleResultResponse(true, "동의가 철회되었습니다."));
  }

  @DeleteMapping("/mydata/{institutionCode}")
  @AuthLevel(RiskLevel.LEVEL_3)
  @Operation(summary = "마이데이터 연결 해제", description = "특정 기관의 마이데이터 연결을 해제합니다.")
  public ApiResponse<SimpleResultResponse> unlinkMyData(
      @RequestParam String userId, @PathVariable String institutionCode) {
    myDataAuthUseCase.unlinkInstitution(userId, institutionCode);
    return ApiResponse.ok(new SimpleResultResponse(true, "마이데이터 연결이 해제되었습니다."));
  }

  @DeleteMapping("/social/{provider}")
  @AuthLevel(RiskLevel.LEVEL_3)
  @Operation(summary = "소셜 로그인 연결 해제", description = "카카오/네이버/애플 소셜 계정 연결을 해제합니다.")
  public ApiResponse<SimpleResultResponse> unlinkSocial(
      @RequestParam String userId, @PathVariable String provider) {
    consentManagementUseCase.unlinkSocial(userId, provider);
    return ApiResponse.ok(new SimpleResultResponse(true, "소셜 로그인 연결이 해제되었습니다."));
  }

  @DeleteMapping("/biometric")
  @AuthLevel(RiskLevel.LEVEL_3)
  @Operation(summary = "생체 인증 해제", description = "사용자의 모든 활성 생체 credential을 폐기합니다.")
  public ApiResponse<SimpleResultResponse> revokeBiometric(@RequestParam String userId) {
    consentManagementUseCase.revokeBiometric(userId);
    return ApiResponse.ok(new SimpleResultResponse(true, "동의가 철회되었습니다."));
  }
}
