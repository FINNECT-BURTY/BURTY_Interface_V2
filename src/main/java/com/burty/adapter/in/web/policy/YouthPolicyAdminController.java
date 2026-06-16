/**
 *
 *
 * <pre>
 * <b>Description  : 정책 API 컨트롤러 (YouthPolicyAdminController)</b>
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

import com.burty.application.dto.transaction.SyncCountResponse;
import com.burty.application.port.in.policy.YouthPolicyUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/youth-policies")
@Tag(name = "Admin - Youth Policy", description = "청년정책 동기화 (관리자 전용)")
@RequiredArgsConstructor
public class YouthPolicyAdminController extends BaseController {

  private final YouthPolicyUseCase youthPolicyUseCase;

  @PostMapping("/sync")
  @Operation(
      summary = "청년정책 동기화 (관리자)",
      description = "온통청년 OpenAPI에서 정책을 가져와 DB에 upsert합니다. 파라미터 미입력 시 전체 조회합니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  public ApiResponse<SyncCountResponse> sync(
      @RequestParam(required = false) String zipCd,
      @RequestParam(required = false) String lclsfNm,
      @RequestParam(required = false) String keyword) {
    int count = youthPolicyUseCase.syncPolicies(zipCd, lclsfNm, keyword);
    return ApiResponse.ok(new SyncCountResponse(count));
  }
}
