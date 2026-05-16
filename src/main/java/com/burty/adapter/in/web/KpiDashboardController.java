package com.burty.adapter.in.web;

import com.burty.core.controller.BaseController;

import com.burty.application.port.in.KpiDashboardUseCase;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/kpi")
@Tag(name = "BURTY KPI Dashboard", description = "행동 채택률·예측 정확도·위험단계 분포 대시보드")
public class KpiDashboardController extends BaseController {

    private final KpiDashboardUseCase useCase;

    public KpiDashboardController(KpiDashboardUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/user/{userId}")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "사용자 KPI", description = "행동 채택률·예측 정확도·위험단계 분포·점수 Top5")
    public ApiResponse<Map<String, Object>> userKpi(@PathVariable String userId) {
        return ApiResponse.ok(useCase.userKpi(userId));
    }

    @GetMapping("/global")
    @AuthLevel(RiskLevel.LEVEL_3)
    @Operation(summary = "전체 KPI", description = "관리자용 글로벌 카운트")
    public ApiResponse<Map<String, Object>> globalKpi() {
        return ApiResponse.ok(useCase.globalKpi());
    }
}
