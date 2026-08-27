package com.burty.application.service.admin;

import com.burty.application.service.finance.TransferLimitGuard;
import com.burty.application.service.finance.TransferOrderWriter;
import com.burty.application.service.support.AuditLogger;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.finance.entity.TransferOrderEntity;
import com.burty.domain.finance.model.ReconciliationCandidate;
import com.burty.domain.finance.repository.TransferOrderRepository;
import com.burty.domain.outbox.entity.OutboxEventEntity;
import com.burty.domain.outbox.entity.OutboxEventEntity.Status;
import com.burty.domain.outbox.repository.OutboxEventRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영 조치 서비스.
 *
 * <p>알람만 있고 조치 수단이 없으면 알람은 소음이 된다. 이 서비스는 Grafana 알람이 울렸을 때 담당자가 실제로 할 수 있는 행동을 제공한다.
 *
 * <ul>
 *   <li>{@code OutboxDeadLetter} 알람 → 원인 수정 후 재처리
 *   <li>{@code TransferResultUnknown} 알람 → 은행 원장 대조 후 수동 확정
 * </ul>
 *
 * <p>수동 확정은 <b>돈의 상태를 사람이 바꾸는 행위</b>다. 모든 호출을 감사 로그에 남기고, 확정 근거(은행 거래번호 또는 사유)를 반드시 받는다.
 */
@Service
public class OperationsService {

  private static final Logger log = LoggerFactory.getLogger(OperationsService.class);

  private final OutboxEventRepository outboxEventRepository;
  private final TransferOrderRepository transferOrderRepository;
  private final TransferOrderWriter orderWriter;
  private final TransferLimitGuard limitGuard;
  private final AuditLogger auditLogger;
  private final Clock clock;

  public OperationsService(
      OutboxEventRepository outboxEventRepository,
      TransferOrderRepository transferOrderRepository,
      TransferOrderWriter orderWriter,
      TransferLimitGuard limitGuard,
      AuditLogger auditLogger,
      Clock clock) {
    this.outboxEventRepository = outboxEventRepository;
    this.transferOrderRepository = transferOrderRepository;
    this.orderWriter = orderWriter;
    this.limitGuard = limitGuard;
    this.auditLogger = auditLogger;
    this.clock = clock;
  }

  // ── 아웃박스 DLQ ──────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public List<OutboxEventEntity> deadLetters(int limit) {
    return outboxEventRepository.findByStatusOrderByEventIdAsc(
        Status.DEAD, Limit.of(Math.min(Math.max(1, limit), 500)));
  }

  @Transactional(readOnly = true)
  public long deadLetterCount() {
    return outboxEventRepository.countByStatus(Status.DEAD);
  }

  /**
   * DEAD 이벤트를 다시 발행 대기로 돌린다.
   *
   * <p>원인을 고치지 않고 재처리하면 다시 DEAD 가 될 뿐이다. 그래서 시도 횟수를 0 으로 되돌려 재시도 예산을 새로 준다.
   */
  @Transactional
  public int redriveDeadLetters(String operatorId, List<Long> eventIds) {
    if (eventIds == null || eventIds.isEmpty()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "재처리할 이벤트 ID가 필요합니다.");
    }
    LocalDateTime now = LocalDateTime.now(clock);
    int redriven = 0;
    for (Long eventId : eventIds) {
      OutboxEventEntity event = outboxEventRepository.findById(eventId).orElse(null);
      if (event == null || event.getStatus() != Status.DEAD) {
        continue;
      }
      event.setStatus(Status.PENDING);
      event.setAttempts(0);
      event.setNextAttemptAt(now);
      event.setLastError(null);
      outboxEventRepository.save(event);
      redriven++;
    }
    auditLogger.logSuccess(
        operatorId,
        "OUTBOX_REDRIVE",
        String.valueOf(eventIds),
        "redriven=%d/%d".formatted(redriven, eventIds.size()));
    log.warn("아웃박스 DEAD 이벤트 재처리 operator={} redriven={}", operatorId, redriven);
    return redriven;
  }

  // ── 이체 수동 확정 ─────────────────────────────────────────────────────────

  /** 정산이 확정하지 못한 미결 이체 목록. */
  @Transactional(readOnly = true)
  public List<ReconciliationCandidate> pendingReconciliation(int limit) {
    return transferOrderRepository.findReconciliationCandidates(
        LocalDateTime.now(clock).plusYears(1), // 예정 시각과 무관하게 전부 본다
        LocalDateTime.now(clock),
        Limit.of(Math.min(Math.max(1, limit), 500)));
  }

  /**
   * 은행 원장 대조 결과를 사람이 확정한다.
   *
   * @param executed true = 실제로 출금됨, false = 출금되지 않음
   * @param evidence 은행 거래번호 또는 대조 근거. 감사 추적을 위해 필수다.
   */
  @Transactional
  public void confirmTransferOutcome(
      String operatorId, Long orderId, boolean executed, String evidence) {
    if (evidence == null || evidence.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "확정 근거(은행 거래번호 또는 대조 사유)가 필요합니다.");
    }
    TransferOrderEntity order =
        transferOrderRepository
            .findById(orderId)
            .orElseThrow(
                () -> new BusinessException(ErrorCode.TRANSFER_NOT_FOUND, "이체 주문을 찾을 수 없습니다."));

    if (order.getStatus().isTerminal()) {
      throw new BusinessException(
          ErrorCode.OPERATION_NOT_ALLOWED,
          "이미 확정된 이체입니다. (현재 상태: %s)".formatted(order.getStatus()));
    }

    String userId = String.valueOf(order.getUser().getUserId());
    if (executed) {
      orderWriter.markExecuted(orderId, evidence);
      // 출금됐으므로 한도는 그대로 소비된 상태로 둔다.
    } else {
      orderWriter.markFailed(orderId, "수동 확정: 미출금 (" + evidence + ")");
      limitGuard.release(userId, order.getAmount(), order.getRequestedAt().toLocalDate());
    }

    auditLogger.logSuccess(
        operatorId,
        "TRANSFER_MANUAL_CONFIRM",
        String.valueOf(orderId),
        "executed=%s, evidence=%s, userId=%s".formatted(executed, evidence, userId));
    log.warn(
        "이체 수동 확정 operator={} orderId={} executed={} evidence={}",
        operatorId,
        orderId,
        executed,
        evidence);
  }
}
