package com.burty.application.service.batch;

import com.burty.application.port.out.bank.OpenBankingPort;
import com.burty.application.port.out.bank.TransferStatus;
import com.burty.application.service.finance.TransferLimitGuard;
import com.burty.application.service.finance.TransferOrderWriter;
import com.burty.application.service.support.AuditLogger;
import com.burty.config.TransferPolicyProperties;
import com.burty.domain.finance.model.ReconciliationCandidate;
import com.burty.domain.finance.repository.TransferOrderRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 이체 정산(reconciliation).
 *
 * <p>은행 호출은 세 가지로 끝난다: 성공, 명확한 거절, <b>결과 불명</b>. 앞의 둘은 즉시 확정되지만 세 번째는 애플리케이션 혼자서는 판단할 수 없다. 타임아웃이
 * 났을 때 실제로 출금됐는지 아닌지는 은행만 안다.
 *
 * <p>이 배치가 그 미결 건을 은행에 다시 물어 확정한다. 이것이 없으면 결과 불명 건은 영원히 미결로 남고, 사용자는 돈이 나갔는지도 모른 채 재시도해서 이중 출금을
 * 만든다. 정산은 선택이 아니라 이체 기능의 필수 구성요소다.
 *
 * <p>대상:
 *
 * <ul>
 *   <li>{@code UNKNOWN} — 응답을 못 받은 건
 *   <li>{@code EXECUTING} — 은행 요청 직후 프로세스가 죽어 상태가 멈춘 건
 * </ul>
 */
@Service
public class TransferReconciliationBatch {

  private static final Logger log = LoggerFactory.getLogger(TransferReconciliationBatch.class);
  private static final int BATCH_SIZE = 100;

  private final TransferOrderRepository transferOrderRepository;
  private final TransferOrderWriter orderWriter;
  private final TransferLimitGuard transferLimitGuard;
  private final OpenBankingPort openBankingPort;
  private final TransferPolicyProperties policy;
  private final AuditLogger auditLogger;
  private final Clock clock;
  private final ObjectProvider<MeterRegistry> meterRegistry;

  public TransferReconciliationBatch(
      TransferOrderRepository transferOrderRepository,
      TransferOrderWriter orderWriter,
      TransferLimitGuard transferLimitGuard,
      OpenBankingPort openBankingPort,
      TransferPolicyProperties policy,
      AuditLogger auditLogger,
      Clock clock,
      ObjectProvider<MeterRegistry> meterRegistry) {
    this.transferOrderRepository = transferOrderRepository;
    this.orderWriter = orderWriter;
    this.transferLimitGuard = transferLimitGuard;
    this.openBankingPort = openBankingPort;
    this.policy = policy;
    this.auditLogger = auditLogger;
    this.clock = clock;
    this.meterRegistry = meterRegistry;
  }

  /**
   * 스케줄 진입점. 락·스케줄링 관심사만 담당하고 처리는 {@link #reconcileOnce()} 에 위임한다.
   *
   * <p>이렇게 나누지 않으면 테스트에서 이 메서드를 직접 호출해도 ShedLock 이 락을 잡아 아무것도 처리하지 않는다. 정산은 돈이 걸린 경로라 반드시 테스트로 검증할
   * 수 있어야 한다.
   */
  @Scheduled(fixedDelayString = "${burty.transfer.reconcile-interval-ms:30000}")
  @SchedulerLock(name = "transferReconciliation", lockAtMostFor = "PT10M", lockAtLeastFor = "PT5S")
  public void reconcile() {
    reconcileOnce();
  }

  /**
   * 정산 대상을 한 배치 처리한다.
   *
   * @return 조회된 대상 건수
   */
  public int reconcileOnce() {
    LocalDateTime now = LocalDateTime.now(clock);
    List<ReconciliationCandidate> candidates = loadCandidates(now);
    if (candidates.isEmpty()) {
      return 0;
    }
    log.info("이체 정산 시작 — 대상 {}건", candidates.size());
    for (ReconciliationCandidate candidate : candidates) {
      try {
        settle(candidate, now);
      } catch (RuntimeException e) {
        // 한 건이 실패해도 나머지는 계속 처리한다.
        log.error("이체 정산 실패 orderId={} reason={}", candidate.orderId(), e.getMessage(), e);
      }
    }
    return candidates.size();
  }

  /** 은행 조회는 트랜잭션 밖에서 하므로 후보를 프로젝션으로 평탄화해 가져온다. */
  private List<ReconciliationCandidate> loadCandidates(LocalDateTime now) {
    LocalDateTime stuckBefore = now.minusSeconds(policy.getStuckExecutingSeconds());
    return transferOrderRepository.findReconciliationCandidates(
        now, stuckBefore, Limit.of(BATCH_SIZE));
  }

  private void settle(ReconciliationCandidate candidate, LocalDateTime now) {
    TransferStatus status =
        openBankingPort.getTransferStatus(candidate.userIdAsString(), candidate.idempotencyKey());

    switch (status.outcome()) {
      case COMPLETED -> {
        orderWriter.markExecuted(candidate.orderId(), status.bankTransactionId());
        auditLogger.logSuccess(
            candidate.userIdAsString(),
            "TRANSFER_RECONCILED",
            String.valueOf(candidate.orderId()),
            "정산 결과: 은행 처리 완료");
        count("completed");
        log.info(
            "정산 확정(완료) orderId={} bankTxnId={}", candidate.orderId(), status.bankTransactionId());
      }
      case REJECTED, NOT_FOUND -> {
        // 출금이 없었음이 확인됐다. 이때만 한도를 되돌린다.
        orderWriter.markFailed(candidate.orderId(), "정산 결과: " + status.reason());
        transferLimitGuard.release(
            candidate.userIdAsString(), candidate.amount(), candidate.requestedAt().toLocalDate());
        auditLogger.logFailure(
            candidate.userIdAsString(),
            "TRANSFER_RECONCILED",
            String.valueOf(candidate.orderId()),
            "정산 결과: 미출금 확정 (" + status.reason() + ")");
        count("failed");
        log.info("정산 확정(미출금) orderId={} reason={}", candidate.orderId(), status.reason());
      }
      case PENDING, UNRESOLVED -> reschedule(candidate, now, status);
    }
  }

  private void reschedule(
      ReconciliationCandidate candidate, LocalDateTime now, TransferStatus status) {
    int attempts = candidate.attemptsOrZero() + 1;
    LocalDateTime next = now.plusSeconds(policy.getReconcileRetryDelaySeconds());
    orderWriter.markUnknownRetry(candidate.orderId(), status.reason(), next, attempts);
    if (attempts >= policy.getReconcileMaxAttempts()) {
      // 자동으로는 못 푼다. 사람이 은행 원장과 대조해야 하는 건이다.
      log.error(
          "이체 정산 미확정 — 수동 대조 필요 orderId={} userId={} amount={} attempts={}",
          candidate.orderId(),
          candidate.userIdAsString(),
          candidate.amount(),
          attempts);
      count("manual-review");
    } else {
      count("rescheduled");
    }
  }

  private void count(String outcome) {
    MeterRegistry registry = meterRegistry.getIfAvailable();
    if (registry != null) {
      registry.counter("burty.transfer.reconciliation", "outcome", outcome).increment();
    }
  }
}
