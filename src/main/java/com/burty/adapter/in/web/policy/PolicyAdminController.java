/**
 *
 *
 * <pre>
 * <b>Description  : 정책 API 컨트롤러 (PolicyAdminController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.policy
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
package com.burty.adapter.in.web.policy;

import com.burty.application.dto.policy.PolicyAdminRequest;
import com.burty.application.dto.policy.PolicyAdminResponse;
import com.burty.application.dto.shared.SimpleResultResponse;
import com.burty.application.port.in.policy.PolicyAdminUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/policies")
@RequiredArgsConstructor
@Tag(name = "BURTY Policy Admin", description = "정책 데이터 최신화/관리자 API")
public class PolicyAdminController extends BaseController {

  private final PolicyAdminUseCase policyAdminUseCase;

  @GetMapping
  @AuthLevel(RiskLevel.LEVEL_3)
  public ApiResponse<List<PolicyAdminResponse>> policies() {
    return ApiResponse.ok(policyAdminUseCase.listPolicies());
  }

  @PostMapping
  @AuthLevel(RiskLevel.LEVEL_3)
  @Operation(summary = "정책 등록/수정", description = "정책명, 신청 URL, 지역/소득/나이 조건, 만료일을 등록 또는 수정합니다.")
  public ApiResponse<PolicyAdminResponse> upsert(@Valid @RequestBody PolicyAdminRequest request) {
    return ApiResponse.ok(policyAdminUseCase.upsert(request));
  }

  @DeleteMapping("/{policyCode}")
  @AuthLevel(RiskLevel.LEVEL_3)
  public ApiResponse<SimpleResultResponse> deactivate(@PathVariable String policyCode) {
    policyAdminUseCase.deactivate(policyCode);
    return ApiResponse.ok(new SimpleResultResponse(true, "정책이 비활성화되었습니다."));
  }
}
