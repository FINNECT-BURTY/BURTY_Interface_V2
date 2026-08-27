package com.burty.application.service.family;

import com.burty.application.port.out.outbox.OutboxPublisher;
import com.burty.application.service.support.AuditLogger;
import com.burty.config.TransferPolicyProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.family.entity.GuardianLinkEntity;
import com.burty.domain.family.entity.TransferApprovalEntity;
import com.burty.domain.family.entity.TransferApprovalEntity.Status;
import com.burty.domain.family.repository.GuardianLinkRepository;
import com.burty.domain.family.repository.TransferApprovalRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이체 보호자 사전 승인.
 *
 * <p>기존 가족 보호 기능은 이상 이체를 <b>사후에 알리기만</b> 했다. 알림 시점에는 이미 출금이 끝난 뒤라 피해를 막지 못한다. 시니어 대상 금융 서비스에서 실제로
 * 필요한 것은 통지가 아니라 <b>차단</b>이다.
 *
 * <p>동작: 보호자가 {@code VIEW_ALERT_AND_APPROVE} 권한을 가진 사용자가 임계 금액 이상을 이체하려 하면, 이체를 실행하지 않고 보류한 뒤 보호자에게
 * 승인 요청을 보낸다. 보호자가 승인하면 그때 실행되고, 거절하거나 기한이 지나면 실행되지 않는다.
 */
@Service
public class TransferApprovalService {

  private static final Logger log = LoggerFactory.getLogger(TransferApprovalService.class);

  private final TransferApprovalRepository approvalRepository;
  private final GuardianLinkRepository guardianLinkRepository;
  private final OutboxPublisher outboxPublisher;
  private final AuditLogger auditLogger;
  private final TransferPolicyProperties policy;
  private final Clock clock;

  public TransferApprovalService(
      TransferApprovalRepository approvalRepository,
      GuardianLinkRepository guardianLinkRepository,
      OutboxPublisher outboxPublisher,
      AuditLogger auditLogger,
      TransferPolicyProperties policy,
      Clock clock) {
    this.approvalRepository = approvalRepository;
    this.guardianLinkRepository = guardianLinkRepository;
    this.outboxPublisher = outboxPublisher;
    this.auditLogger = auditLogger;
    this.policy = policy;
    this.clock = clock;
  }

  /**
   * 이 이체가 보호자 승인을 받아야 하는가.
   *
   * <p>승인 권한을 가진 활성 보호자가 있고, 금액이 임계치 이상일 때만 true.
   */
  @Transactional(readOnly = true)
  public Optional<String> requiredGuardian(String userId, long amount) {
    if (!policy.isApprovalEnabled() || amount < policy.getApprovalThreshold()) {
      return Optional.empty();
    }
    List<GuardianLinkEntity> links =
        guardianLinkRepository.findBySeniorUser_UserIdAndStatusAndPermission(
            Long.parseLong(userId),
            GuardianLinkEntity.LinkStatus.ACTIVE,
            GuardianLinkEntity.Permission.VIEW_ALERT_AND_APPROVE);
    return links.stream()
        .findFirst()
        .map(link -> String.valueOf(link.getGuardianUser().getUserId()));
  }

  /** 승인 요청 생성 + 보호자 알림 발행. 이체 주문과 같은 흐름에서 호출된다. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public TransferApprovalEntity requestApproval(
      Long orderId,
      String requesterUserId,
      String guardianUserId,
      long amount,
      String toAccountMasked,
      String reason) {

    LocalDateTime now = LocalDateTime.now(clock);
    TransferApprovalEntity approval = new TransferApprovalEntity();
    approval.setOrderId(orderId);
    approval.setRequesterUserId(requesterUserId);
    approval.setGuardianUserId(guardianUserId);
    approval.setAmount(amount);
    approval.setToAccountMasked(toAccountMasked);
    approval.setReason(reason);
    approval.setStatus(Status.PENDING);
    approval.setRequestedAt(now);
    approval.setExpiresAt(now.plusMinutes(policy.getApprovalExpiryMinutes()));
    approvalRepository.save(approval);

    outboxPublisher.publish(
        "TransferApproval",
        String.valueOf(approval.getApprovalId()),
        TransferApprovalOutboxHandler.REQUESTED_EVENT,
        Map.of(
            "guardianUserId", guardianUserId,
            "requesterUserId", requesterUserId,
            "amount", amount,
            "toAccountMasked", toAccountMasked,
            "expiresAt", String.valueOf(approval.getExpiresAt())));

    auditLogger.logSuccess(
        requesterUserId,
        "TRANSFER_APPROVAL_REQUESTED",
        String.valueOf(orderId),
        "guardian=%s, amount=%d".formatted(guardianUserId, amount));
    log.info(
        "이체 보호자 승인 요청 orderId={} requester={} guardian={} amount={}",
        orderId,
        requesterUserId,
        guardianUserId,
        amount);
    return approval;
  }

  /** 보호자 승인. 실제 이체 실행은 호출자가 이어서 수행한다. */
  @Transactional
  public TransferApprovalEntity approve(String guardianUserId, Long approvalId, String note) {
    TransferApprovalEntity approval = loadDecidable(guardianUserId, approvalId);
    approval.setStatus(Status.APPROVED);
    approval.setDecidedAt(LocalDateTime.now(clock));
    approval.setDecisionNote(note);
    approvalRepository.save(approval);

    auditLogger.logSuccess(
        guardianUserId,
        "TRANSFER_APPROVAL_APPROVED",
        String.valueOf(approval.getOrderId()),
        "requester=" + approval.getRequesterUserId());
    return approval;
  }

  @Transactional
  public TransferApprovalEntity reject(String guardianUserId, Long approvalId, String note) {
    TransferApprovalEntity approval = loadDecidable(guardianUserId, approvalId);
    approval.setStatus(Status.REJECTED);
    approval.setDecidedAt(LocalDateTime.now(clock));
    approval.setDecisionNote(note);
    approvalRepository.save(approval);

    outboxPublisher.publish(
        "TransferApproval",
        String.valueOf(approval.getApprovalId()),
        TransferApprovalOutboxHandler.DECIDED_EVENT,
        Map.of(
            "userId",
            approval.getRequesterUserId(),
            "decision",
            "REJECTED",
            "amount",
            approval.getAmount(),
            "note",
            note == null ? "" : note));

    auditLogger.logSuccess(
        guardianUserId,
        "TRANSFER_APPROVAL_REJECTED",
        String.valueOf(approval.getOrderId()),
        "requester=" + approval.getRequesterUserId());
    return approval;
  }

  @Transactional(readOnly = true)
  public List<TransferApprovalEntity> pendingForGuardian(String guardianUserId) {
    return approvalRepository.findByGuardianUserIdAndStatusOrderByApprovalIdDesc(
        guardianUserId, Status.PENDING);
  }

  @Transactional(readOnly = true)
  public List<TransferApprovalEntity> myRequests(String requesterUserId) {
    return approvalRepository.findByRequesterUserIdOrderByApprovalIdDesc(requesterUserId);
  }

  /** 기한 만료 처리. 보류된 이체가 영원히 남지 않게 한다. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean expire(Long approvalId, LocalDateTime now) {
    TransferApprovalEntity approval = approvalRepository.findById(approvalId).orElse(null);
    if (approval == null || approval.getStatus() != Status.PENDING) {
      return false;
    }
    approval.setStatus(Status.EXPIRED);
    approval.setDecidedAt(now);
    approval.setDecisionNote("승인 기한 만료");
    approvalRepository.save(approval);

    outboxPublisher.publish(
        "TransferApproval",
        String.valueOf(approval.getApprovalId()),
        TransferApprovalOutboxHandler.DECIDED_EVENT,
        Map.of(
            "userId",
            approval.getRequesterUserId(),
            "decision",
            "EXPIRED",
            "amount",
            approval.getAmount(),
            "note",
            "보호자 승인 기한이 지나 이체가 취소되었습니다."));
    return true;
  }

  private TransferApprovalEntity loadDecidable(String guardianUserId, Long approvalId) {
    TransferApprovalEntity approval =
        approvalRepository
            .findByApprovalIdAndGuardianUserId(approvalId, guardianUserId)
            .orElseThrow(
                () -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "승인 요청을 찾을 수 없습니다."));
    if (approval.getStatus() != Status.PENDING) {
      throw new BusinessException(
          ErrorCode.OPERATION_NOT_ALLOWED,
          "이미 처리된 요청입니다. (현재 상태: %s)".formatted(approval.getStatus()));
    }
    if (approval.getExpiresAt().isBefore(LocalDateTime.now(clock))) {
      throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "승인 기한이 지난 요청입니다.");
    }
    return approval;
  }
}
