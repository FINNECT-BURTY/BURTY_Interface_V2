package com.burty.adapter.in.web;

import com.burty.application.port.in.YouthPolicyUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/youth-policies")
@Tag(name = "Admin - Youth Policy", description = "청년정책 동기화 (관리자 전용)")
public class YouthPolicyAdminController extends BaseController {

    private final YouthPolicyUseCase youthPolicyUseCase;

    public YouthPolicyAdminController(YouthPolicyUseCase youthPolicyUseCase) {
        this.youthPolicyUseCase = youthPolicyUseCase;
    }

    @PostMapping("/sync")
    @Operation(
            summary = "청년정책 동기화 (관리자)",
            description = "온통청년 OpenAPI에서 정책을 가져와 DB에 upsert합니다. 파라미터 미입력 시 전체 조회합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    public ApiResponse<Map<String, Object>> sync(
            @RequestParam(required = false) String zipCd,
            @RequestParam(required = false) String lclsfNm,
            @RequestParam(required = false) String keyword
    ) {
        int count = youthPolicyUseCase.syncPolicies(zipCd, lclsfNm, keyword);
        return ApiResponse.ok(Map.of("syncedCount", count));
    }
}
