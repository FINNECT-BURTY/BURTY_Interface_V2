/**
 *
 *
 * <pre>
 * <b>Description  : 현금흐름 API 컨트롤러 (KpiDashboardController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.cashflow
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
package com.burty.adapter.in.web.cashflow;

import com.burty.application.dto.cashflow.GlobalKpiResponse;
import com.burty.application.dto.cashflow.UserKpiResponse;
import com.burty.application.port.in.cashflow.KpiDashboardUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/kpi")
@Tag(name = "BURTY KPI Dashboard", description = "행동 채택률·예측 정확도·위험단계 분포 대시보드")
@RequiredArgsConstructor
public class KpiDashboardController extends BaseController {

  private final KpiDashboardUseCase useCase;

  @GetMapping("/user/{userId}")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "사용자 KPI", description = "행동 채택률·예측 정확도·위험단계 분포·점수 Top5")
  public ApiResponse<UserKpiResponse> userKpi(@PathVariable String userId) {
    return ApiResponse.ok(useCase.userKpi(userId));
  }

  @GetMapping("/global")
  @AuthLevel(RiskLevel.LEVEL_3)
  @Operation(summary = "전체 KPI", description = "관리자용 글로벌 카운트")
  public ApiResponse<GlobalKpiResponse> globalKpi() {
    return ApiResponse.ok(useCase.globalKpi());
  }
}
