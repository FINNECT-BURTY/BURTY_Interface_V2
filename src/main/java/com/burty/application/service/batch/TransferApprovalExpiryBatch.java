package com.burty.application.service.batch;

import com.burty.application.service.family.TransferApprovalService;
import com.burty.application.service.finance.TransferOrderWriter;
import com.burty.domain.family.entity.TransferApprovalEntity;
import com.burty.domain.family.entity.TransferApprovalEntity.Status;
import com.burty.domain.family.repository.TransferApprovalRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 보호자 승인 기한 만료 처리.
 *
 * <p>승인 대기 상태로 무한정 남으면 사용자는 이체가 왜 안 되는지 모른 채 방치된다. 기한이 지나면 승인 요청을 만료시키고 이체 주문도 함께 취소해, 사용자가 다시 시도할
 * 수 있게 한다.
 */
@Service
public class TransferApprovalExpiryBatch {

  private static final Logger log = LoggerFactory.getLogger(TransferApprovalExpiryBatch.class);

  private final TransferApprovalRepository approvalRepository;
  private final TransferApprovalService approvalService;
  private final TransferOrderWriter orderWriter;
  private final Clock clock;

  public TransferApprovalExpiryBatch(
      TransferApprovalRepository approvalRepository,
      TransferApprovalService approvalService,
      TransferOrderWriter orderWriter,
      Clock clock) {
    this.approvalRepository = approvalRepository;
    this.approvalService = approvalService;
    this.orderWriter = orderWriter;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${burty.transfer.approval-expiry-interval-ms:60000}")
  @SchedulerLock(name = "transferApprovalExpiry", lockAtMostFor = "PT10M", lockAtLeastFor = "PT5S")
  public void expireStaleApprovals() {
    LocalDateTime now = LocalDateTime.now(clock);
    List<TransferApprovalEntity> stale =
        approvalRepository.findByStatusAndExpiresAtLessThanEqual(Status.PENDING, now);
    if (stale.isEmpty()) {
      return;
    }
    log.info("보호자 승인 기한 만료 처리 — 대상 {}건", stale.size());

    for (TransferApprovalEntity approval : stale) {
      try {
        if (approvalService.expire(approval.getApprovalId(), now)) {
          // 승인받지 못한 이체 주문은 취소한다. 보류 상태로 남겨두면 멱등키가 계속 점유된다.
          orderWriter.markCancelled(approval.getOrderId(), "보호자 승인 기한 만료");
        }
      } catch (RuntimeException e) {
        log.error(
            "승인 만료 처리 실패 approvalId={} orderId={} reason={}",
            approval.getApprovalId(),
            approval.getOrderId(),
            e.getMessage(),
            e);
      }
    }
  }
}
