/**
 *
 *
 * <pre>
 * <b>Description  : 가족보호 API 컨트롤러 (FamilyProtectionController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.family
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
package com.burty.adapter.in.web.family;

import com.burty.adapter.in.web.mapper.WebResponseMapper;
import com.burty.adapter.out.alert.FamilyAlertSseBroker;
import com.burty.application.dto.family.FamilyAlertResponse;
import com.burty.application.dto.family.FamilyConsentRequest;
import com.burty.application.dto.family.FamilyConsentResponse;
import com.burty.application.dto.family.FamilyConsentUpdateRequest;
import com.burty.application.dto.family.FamilyDashboardResponse;
import com.burty.application.dto.shared.FlagResultResponse;
import com.burty.application.port.in.family.FamilyProtectionUseCase;
import com.burty.core.annotation.CurrentUserId;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@Tag(name = "BURTY Family Protection", description = "가족 알림 및 동의 관리 API")
public class FamilyProtectionController extends BaseController {

  private final FamilyProtectionUseCase familyProtectionUseCase;
  private final WebResponseMapper webResponseMapper;
  private final FamilyAlertSseBroker familyAlertSseBroker;

  @GetMapping("/family-alerts")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<List<FamilyAlertResponse>> familyAlerts(@CurrentUserId String userId) {
    return ApiResponse.ok(
        webResponseMapper.toFamilyAlertResponses(familyProtectionUseCase.getFamilyAlerts(userId)));
  }

  @GetMapping("/family-alerts/stream")
  @AuthLevel(RiskLevel.LEVEL_1)
  public SseEmitter familyAlertStream(@CurrentUserId String userId) {
    return familyAlertSseBroker.subscribe(userId);
  }

  @PostMapping("/family/consents")
  @AuthLevel(RiskLevel.LEVEL_2)
  public ApiResponse<FlagResultResponse> registerFamilyConsent(
      @Valid @RequestBody FamilyConsentRequest request) {
    familyProtectionUseCase.registerFamilyConsent(request.parentUserId(), request.childUserId());
    return ApiResponse.ok(FlagResultResponse.of("registered", true));
  }

  @PatchMapping("/family/consents")
  @AuthLevel(RiskLevel.LEVEL_2)
  public ApiResponse<FlagResultResponse> updateFamilyConsent(
      @Valid @RequestBody FamilyConsentUpdateRequest request) {
    boolean updated =
        familyProtectionUseCase.updateFamilyConsent(
            request.parentUserId(), request.childUserId(), request.consented());
    return ApiResponse.ok(FlagResultResponse.of("updated", updated));
  }

  @DeleteMapping("/family/consents")
  @AuthLevel(RiskLevel.LEVEL_2)
  public ApiResponse<FlagResultResponse> revokeFamilyConsent(
      @RequestParam String parentUserId, @RequestParam String childUserId) {
    boolean deleted = familyProtectionUseCase.revokeFamilyConsent(parentUserId, childUserId);
    return ApiResponse.ok(FlagResultResponse.of("deleted", deleted));
  }

  @GetMapping("/family/consents")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<List<FamilyConsentResponse>> familyConsents(
      @RequestParam String parentUserId) {
    return ApiResponse.ok(
        webResponseMapper.toFamilyConsentResponses(
            familyProtectionUseCase.getFamilyConsents(parentUserId)));
  }

  @GetMapping("/family/dashboard")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<FamilyDashboardResponse> familyDashboard(@CurrentUserId String userId) {
    return ApiResponse.ok(
        webResponseMapper.toResponse(familyProtectionUseCase.getFamilyDashboardSummary(userId)));
  }
}
