/**
 *
 *
 * <pre>
 * <b>Description  : 현금흐름 API 컨트롤러 (CashflowManagementController)</b>
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

import com.burty.application.dto.cashflow.CashflowCalendarDayResponse;
import com.burty.application.dto.cashflow.CashflowScheduleRequest;
import com.burty.application.dto.cashflow.CashflowScheduleResponse;
import com.burty.application.dto.cashflow.RiskCauseResponse;
import com.burty.application.dto.shared.SimpleResultResponse;
import com.burty.application.port.in.cashflow.CashflowManagementUseCase;
import com.burty.core.annotation.CurrentUserId;
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
@RequestMapping("/cashflow-management")
@RequiredArgsConstructor
@Tag(name = "BURTY Cashflow Management", description = "현금흐름 캘린더/고정지출/위험원인 관리 API")
public class CashflowManagementController extends BaseController {

  private final CashflowManagementUseCase cashflowManagementUseCase;

  @GetMapping("/calendar")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "현금흐름 캘린더", description = "30일 예상 잔액과 월세/카드/대출/급여 이벤트를 일자별로 반환합니다.")
  public ApiResponse<List<CashflowCalendarDayResponse>> calendar(@CurrentUserId String userId) {
    return ApiResponse.ok(cashflowManagementUseCase.calendar(userId));
  }

  @GetMapping("/schedules")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<List<CashflowScheduleResponse>> schedules(@CurrentUserId String userId) {
    return ApiResponse.ok(cashflowManagementUseCase.schedules(userId));
  }

  @PostMapping("/schedules")
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "고정 수입/지출 등록", description = "월세, 관리비, 통신비, 구독료, 대출 상환일 등을 직접 등록합니다.")
  public ApiResponse<CashflowScheduleResponse> upsertSchedule(
      @Valid @RequestBody CashflowScheduleRequest request) {
    return ApiResponse.ok(cashflowManagementUseCase.upsertSchedule(request));
  }

  @DeleteMapping("/schedules/{scheduleId}")
  @AuthLevel(RiskLevel.LEVEL_2)
  public ApiResponse<SimpleResultResponse> deactivateSchedule(
      @PathVariable String scheduleId, @CurrentUserId String userId) {
    cashflowManagementUseCase.deactivateSchedule(scheduleId, userId);
    return ApiResponse.ok(new SimpleResultResponse(true, "현금흐름 일정이 비활성화되었습니다."));
  }

  @GetMapping("/risk-causes")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "위험 원인 분해", description = "월세/카드/대출/변동지출 중 어떤 요인이 잔액 위험을 키우는지 반환합니다.")
  public ApiResponse<List<RiskCauseResponse>> riskCauses(@CurrentUserId String userId) {
    return ApiResponse.ok(cashflowManagementUseCase.riskCauses(userId));
  }
}
