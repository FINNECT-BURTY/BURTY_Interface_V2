package com.burty.adapter.in.web.admin;

import com.burty.application.service.admin.OperationsService;
import com.burty.core.annotation.CurrentUserId;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.domain.finance.model.ReconciliationCandidate;
import com.burty.domain.outbox.entity.OutboxEventEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 운영 조치 API (ADMIN 전용).
 *
 * <p>Grafana 알람이 울렸을 때 담당자가 실제로 취할 수 있는 조치를 제공한다. 알람만 있고 조치 수단이 없으면 알람은 소음이 된다.
 */
@RestController
@RequestMapping("/admin/ops")
@Tag(name = "BURTY Operations", description = "운영 조치 API (DLQ 재처리·이체 수동 확정)")
@RequiredArgsConstructor
public class OperationsAdminController extends BaseController {

  private final OperationsService operationsService;

  public record RedriveRequest(@NotEmpty(message = "재처리할 이벤트 ID가 필요합니다") List<Long> eventIds) {}

  /** 은행 원장 대조 결과. 근거 없이 돈의 상태를 바꿀 수 없도록 evidence 를 필수로 받는다. */
  public record ConfirmRequest(
      @NotNull(message = "실제 출금 여부를 명시해야 합니다") Boolean executed,
      @Size(min = 1, max = 200, message = "확정 근거는 1~200자여야 합니다") String evidence) {}

  public record DeadLetterView(
      Long eventId,
      String aggregateType,
      String aggregateId,
      String eventType,
      int attempts,
      String lastError,
      String createdAt) {

    static DeadLetterView from(OutboxEventEntity e) {
      return new DeadLetterView(
          e.getEventId(),
          e.getAggregateType(),
          e.getAggregateId(),
          e.getEventType(),
          e.getAttempts() == null ? 0 : e.getAttempts(),
          e.getLastError(),
          String.valueOf(e.getCreatedAt()));
    }
  }

  // ── 아웃박스 DLQ ──────────────────────────────────────────────────────────

  @GetMapping("/outbox/dead")
  @Operation(summary = "DLQ 격리된 이벤트 조회", description = "재시도를 소진해 발행되지 못한 이벤트 목록입니다.")
  public ApiResponse<List<DeadLetterView>> deadLetters(
      @RequestParam(defaultValue = "100") int limit) {
    return ApiResponse.ok(
        operationsService.deadLetters(limit).stream().map(DeadLetterView::from).toList());
  }

  @PostMapping("/outbox/redrive")
  @Operation(summary = "DLQ 이벤트 재처리", description = "원인을 수정한 뒤 호출하세요. 고치지 않고 재처리하면 다시 DLQ로 갑니다.")
  public ApiResponse<Integer> redrive(
      @CurrentUserId String operatorId, @Valid @RequestBody RedriveRequest request) {
    return ApiResponse.ok(operationsService.redriveDeadLetters(operatorId, request.eventIds()));
  }

  // ── 이체 수동 확정 ─────────────────────────────────────────────────────────

  @GetMapping("/transfers/pending-reconciliation")
  @Operation(
      summary = "정산 미확정 이체 조회",
      description = "은행 응답을 확인하지 못해 출금 여부가 불확실한 건입니다. 은행 원장과 대조해야 합니다.")
  public ApiResponse<List<ReconciliationCandidate>> pendingReconciliation(
      @RequestParam(defaultValue = "100") int limit) {
    return ApiResponse.ok(operationsService.pendingReconciliation(limit));
  }

  @PostMapping("/transfers/{orderId}/confirm")
  @Operation(
      summary = "이체 결과 수동 확정",
      description = "은행 원장 대조 후 실제 출금 여부를 확정합니다. 모든 호출이 감사 로그에 남습니다.")
  public ApiResponse<Boolean> confirmTransfer(
      @CurrentUserId String operatorId,
      @PathVariable Long orderId,
      @Valid @RequestBody ConfirmRequest request) {
    operationsService.confirmTransferOutcome(
        operatorId, orderId, request.executed(), request.evidence());
    return ApiResponse.ok(true);
  }
}
