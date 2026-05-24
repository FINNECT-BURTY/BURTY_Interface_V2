package com.burty.adapter.in.web;

import com.burty.adapter.in.web.dto.YouthPolicyResponse;
import com.burty.adapter.in.web.dto.YouthPolicySummary;
import com.burty.application.port.in.YouthPolicyUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/youth-policies")
@Tag(name = "Youth Policy", description = "온통청년 청년정책 조회 API")
public class YouthPolicyController extends BaseController {

    private final YouthPolicyUseCase youthPolicyUseCase;

    public YouthPolicyController(YouthPolicyUseCase youthPolicyUseCase) {
        this.youthPolicyUseCase = youthPolicyUseCase;
    }

    @GetMapping
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(
            summary = "청년정책 목록 조회 (raw 필터)",
            description = "DB에 저장된 청년정책을 필터링하여 페이지 단위로 조회합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    public ApiResponse<Page<YouthPolicyResponse>> search(
            @RequestParam(required = false) String lclsfNm,
            @RequestParam(required = false) String mclsfNm,
            @RequestParam(required = false) String zipCd,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<YouthPolicyResponse> result = youthPolicyUseCase.searchPolicies(
                lclsfNm, mclsfNm, zipCd, keyword, minAge, maxAge,
                PageRequest.of(page, size, Sort.by("id").descending())
        ).map(YouthPolicyResponse::from);
        return ApiResponse.ok(result);
    }

    @GetMapping("/search")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(
            summary = "청년정책 도메인 검색",
            description = """
                    BURTY 앱 도메인 기준으로 청년정책을 검색합니다.

                    **domain 값:**
                    - `finance` — 금융지원 (대출, 이자, 금리 혜택 등)
                    - `housing` — 주거지원 (임대, 전세, 주택 관련)
                    - `welfare` — 복지지원 (생활비, 의료, 문화 관련)
                    - `subsidy` — 지원금·보조금 (장려금, 보조금 직접 지급)
                    - 미입력 시 전체 조회
                    """,
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    public ApiResponse<Page<YouthPolicySummary>> searchByDomain(
            @Parameter(description = "finance / housing / welfare / subsidy")
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<YouthPolicySummary> result = youthPolicyUseCase.searchByDomain(
                domain, keyword, minAge, maxAge,
                PageRequest.of(page, size, Sort.by("id").descending())
        ).map(YouthPolicySummary::from);
        return ApiResponse.ok(result);
    }
}
