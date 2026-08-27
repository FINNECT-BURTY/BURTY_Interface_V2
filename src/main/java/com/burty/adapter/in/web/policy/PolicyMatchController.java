/**
 *
 *
 * <pre>
 * <b>Description  : 정책 API 컨트롤러 (PolicyMatchController)</b>
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

import com.burty.adapter.in.web.mapper.WebResponseMapper;
import com.burty.application.dto.policy.PolicyApplyResponse;
import com.burty.application.dto.policy.PolicyMatchResponse;
import com.burty.application.port.in.policy.PolicyMatchUseCase;
import com.burty.core.annotation.CurrentUserId;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "BURTY Policy Match", description = "청년 정책 매칭 API")
public class PolicyMatchController extends BaseController {

  private final PolicyMatchUseCase policyMatchUseCase;
  private final WebResponseMapper webResponseMapper;

  @GetMapping("/policy/match")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "청년 정책 매칭", description = "사용자 속성과 정책 조건을 매칭해 상위 정책을 추천합니다.")
  public ApiResponse<List<PolicyMatchResponse>> policyMatch(@CurrentUserId String userId) {
    return ApiResponse.ok(
        webResponseMapper.toPolicyMatchResponses(policyMatchUseCase.matchForUser(userId)));
  }

  @PostMapping("/policy/{policyCode}/apply")
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "정책 신청 표시", description = "사용자가 정책을 신청했음을 기록합니다. 신청률 KPI에 반영됩니다.")
  public ApiResponse<PolicyApplyResponse> applyPolicy(
      @CurrentUserId String userId, @PathVariable String policyCode) {
    policyMatchUseCase.applyPolicy(userId, policyCode);
    return ApiResponse.ok(new PolicyApplyResponse(true, policyCode, userId));
  }
}
