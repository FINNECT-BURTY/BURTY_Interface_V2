package com.burty.adapter.in.web.cashflow;

import com.burty.application.service.cashflow.BudgetService;
import com.burty.core.annotation.CurrentUserId;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 예산 설정 및 사용 현황 API.
 *
 * <p>카테고리별 예산과 전체 지출 예산을 동시에 둘 수 있다. {@code categoryCode} 를 비우면 전체 예산이다.
 */
@RestController
@RequestMapping("/budgets")
@Tag(name = "BURTY Budget", description = "예산 설정·현황·초과 경고 API")
@RequiredArgsConstructor
public class BudgetController extends BaseController {

  private final BudgetService budgetService;

  public record BudgetUpsertRequest(
      @Size(max = 40, message = "카테고리 코드는 40자를 넘을 수 없습니다") String categoryCode,
      @Positive(message = "예산은 0보다 커야 합니다") long amount,
      @Min(value = 1, message = "경고 임계치는 1 이상이어야 합니다")
          @Max(value = 100, message = "경고 임계치는 100 이하여야 합니다")
          Integer alertThresholdPercent) {}

  public record BudgetResponse(
      Long budgetId, String categoryCode, long amount, int alertThresholdPercent) {}

  @PutMapping
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "예산 설정", description = "카테고리별(또는 전체) 월 예산을 설정합니다. 이미 있으면 갱신합니다.")
  public ApiResponse<BudgetResponse> upsert(
      @CurrentUserId String userId, @Valid @RequestBody BudgetUpsertRequest request) {
    var budget =
        budgetService.upsert(
            userId, request.categoryCode(), request.amount(), request.alertThresholdPercent());
    return ApiResponse.ok(
        new BudgetResponse(
            budget.getBudgetId(),
            budget.getCategoryCode(),
            budget.getAmount(),
            budget.getAlertThresholdPercent()));
  }

  @GetMapping
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "예산 사용 현황", description = "이번 달 예산 대비 지출 현황을 반환합니다.")
  public ApiResponse<List<BudgetService.BudgetStatus>> status(@CurrentUserId String userId) {
    return ApiResponse.ok(budgetService.currentStatus(userId));
  }

  @DeleteMapping("/{budgetId}")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "예산 해제")
  public ApiResponse<Boolean> deactivate(
      @CurrentUserId String userId, @PathVariable Long budgetId) {
    budgetService.deactivate(userId, budgetId);
    return ApiResponse.ok(true);
  }

  @PostMapping("/evaluate")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(
      summary = "예산 초과 즉시 평가",
      description = "거래 동기화 후 자동으로도 실행되지만, 사용자가 직접 재평가를 요청할 수 있습니다.")
  public ApiResponse<Integer> evaluate(@CurrentUserId String userId) {
    return ApiResponse.ok(budgetService.evaluateAndNotify(userId));
  }
}
