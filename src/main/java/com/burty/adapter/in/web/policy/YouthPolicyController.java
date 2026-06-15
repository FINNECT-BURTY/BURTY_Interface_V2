/**
 *
 *
 * <pre>
 * <b>Description  : 정책 API 컨트롤러 (YouthPolicyController)</b>
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

import com.burty.application.dto.policy.YouthPolicyResponse;
import com.burty.application.port.in.policy.YouthPolicyUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/youth-policies")
@Tag(name = "Youth Policy", description = "온통청년 청년정책 조회 API")
@RequiredArgsConstructor
public class YouthPolicyController extends BaseController {

  private final YouthPolicyUseCase youthPolicyUseCase;

  @GetMapping
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(
      summary = "청년정책 목록 조회 (raw 필터)",
      description = "DB에 저장된 청년정책을 필터링하여 페이지 단위로 조회합니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  public ApiResponse<Page<YouthPolicyResponse>> search(
      @RequestParam(required = false) String lclsfNm,
      @RequestParam(required = false) String mclsfNm,
      @RequestParam(required = false) String zipCd,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer minAge,
      @RequestParam(required = false) Integer maxAge,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Page<YouthPolicyResponse> result =
        youthPolicyUseCase
            .searchPolicies(
                lclsfNm,
                mclsfNm,
                zipCd,
                keyword,
                minAge,
                maxAge,
                PageRequest.of(page, size, Sort.by("id").descending()))
            .map(YouthPolicyResponse::from);
    return ApiResponse.ok(result);
  }
}
