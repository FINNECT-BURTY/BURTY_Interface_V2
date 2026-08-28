package com.burty.application.service.finance;

import com.burty.application.port.out.outbox.OutboxPublisher;
import com.burty.core.constant.AppMessages;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.finance.entity.TransferOrderEntity;
import com.burty.domain.finance.repository.TransferOrderRepository;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.UserRepository;
import com.burty.util.AccountNumberHasher;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이체 주문의 상태 전이 전담. 모든 메서드가 {@code REQUIRES_NEW} 로 <b>즉시 독립 커밋</b>된다.
 *
 * <p>왜 이렇게 하는가. 예전 구현은 은행 호출까지 하나의 {@code @Transactional} 안에 있었다. 그래서
 *
 * <ul>
 *   <li>실패 시 {@code setStatus(FAILED); save(); throw;} 를 해도 롤백되어 <b>실패 기록 자체가 사라졌다.</b>
 *   <li>은행 호출이 성공한 뒤 커밋이 실패하면 <b>돈은 나갔는데 기록이 없었다.</b>
 *   <li>네트워크 왕복 내내 DB 커넥션을 점유했다.
 * </ul>
 *
 * <p>상태 전이를 독립 커밋으로 떼어내면 은행 호출을 트랜잭션 밖에서 할 수 있고, 어느 시점에 죽더라도 DB 에 진행 상태가 남는다.
 */
@Component
public class TransferOrderWriter {

  private final TransferOrderRepository transferOrderRepository;
  private final UserRepository userRepository;
  private final AccountNumberHasher accountNumberHasher;
  private final OutboxPublisher outboxPublisher;
  private final Clock clock;

  public TransferOrderWriter(
      TransferOrderRepository transferOrderRepository,
      UserRepository userRepository,
      AccountNumberHasher accountNumberHasher,
      OutboxPublisher outboxPublisher,
      Clock clock) {
    this.transferOrderRepository = transferOrderRepository;
    this.userRepository = userRepository;
    this.accountNumberHasher = accountNumberHasher;
    this.outboxPublisher = outboxPublisher;
    this.clock = clock;
  }

  /** 멱등키 선점 결과. */
  public record Claim(TransferOrderEntity order, boolean fresh) {}

  /**
   * 멱등키를 <b>INSERT 로 선점</b>한다.
   *
   * <p>예전 구현은 "조회해서 없으면 실행" 이었다. 같은 키의 동시 요청 두 건이 모두 조회에서 empty 를 받아 <b>둘 다 이체를 실행</b>했다. 멱등키가 막아야
   * 할 바로 그 시나리오다. 여기서는 유니크 제약 {@code (user_id, idempotency_key)} 위반을 신호로 삼아 선점 실패를 감지한다. 경쟁은 DB 가
   * 판정하므로 애플리케이션 레벨 경쟁 구간이 없다.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Claim claim(
      String userId, String toAccount, long amount, String description, String idempotencyKey) {
    long numericUserId = Long.parseLong(userId);
    try {
      UserEntity user =
          userRepository
              .findById(numericUserId)
              .orElseThrow(
                  () ->
                      new BusinessException(
                          ErrorCode.USER_NOT_FOUND, AppMessages.Transfer.USER_NOT_FOUND));

      TransferOrderEntity order = new TransferOrderEntity();
      order.setUser(user);
      order.setIdempotencyKey(idempotencyKey);
      order.setToAccountNo(toAccount);
      order.setToAccountNoMasked(accountNumberHasher.mask(toAccount));
      order.setToBankCode("000");
      order.setAmount(amount);
      order.setMemo(description);
      order.setStatus(TransferOrderEntity.Status.PENDING);
      order.setRequestedAt(LocalDateTime.now(clock));
      order.setReconcileAttempts(0);
      transferOrderRepository.saveAndFlush(order);
      return new Claim(order, true);
    } catch (DataIntegrityViolationException e) {
      TransferOrderEntity existing =
          transferOrderRepository
              .findByUser_UserIdAndIdempotencyKey(numericUserId, idempotencyKey)
              .orElseThrow(
                  () ->
                      new BusinessException(
                          ErrorCode.DATA_INTEGRITY_VIOLATION, "멱등키 충돌을 감지했으나 기존 주문을 찾을 수 없습니다", e));
      return new Claim(existing, false);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markStatus(Long orderId, TransferOrderEntity.Status status) {
    load(orderId).setStatus(status);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markExecuted(Long orderId, String bankTransactionId) {
    TransferOrderEntity order = load(orderId);
    order.setStatus(TransferOrderEntity.Status.EXECUTED);
    order.setBankTransactionId(bankTransactionId);
    order.setExecutedAt(LocalDateTime.now(clock));
    order.setFailedReason(null);
    order.setNextReconcileAt(null);
    publishOutcome(order, "TransferExecuted");
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markFailed(Long orderId, String reason) {
    TransferOrderEntity order = load(orderId);
    order.setStatus(TransferOrderEntity.Status.FAILED);
    order.setFailedReason(truncate(reason));
    order.setNextReconcileAt(null);
    publishOutcome(order, "TransferFailed");
  }

  /** 은행 응답을 확인하지 못한 건. 정산 대상으로 표시한다. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markUnknown(Long orderId, String reason, LocalDateTime nextReconcileAt) {
    TransferOrderEntity order = load(orderId);
    order.setStatus(TransferOrderEntity.Status.UNKNOWN);
    order.setFailedReason(truncate(reason));
    order.setNextReconcileAt(nextReconcileAt);
  }

  /**
   * 한도를 차감한 날짜를 주문에 기록한다.
   *
   * <p>해제 시 이 값을 그대로 쓴다. 나중에 날짜를 다시 계산하면 자정을 걸친 이체에서 다른 행을 가리킨다.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markLimitReserved(Long orderId, java.time.LocalDate usageDate) {
    load(orderId).setLimitUsageDate(usageDate);
  }

  /** 사용자/시스템에 의한 취소. 이미 실행된 건은 취소할 수 없다. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markCancelled(Long orderId, String reason) {
    TransferOrderEntity order = load(orderId);
    if (!order.getStatus().isCancellable()) {
      throw new BusinessException(
          ErrorCode.TRANSFER_NOT_CANCELLABLE, AppMessages.Transfer.NOT_CANCELLABLE);
    }
    order.setStatus(TransferOrderEntity.Status.CANCELLED);
    order.setFailedReason(truncate(reason));
    order.setNextReconcileAt(null);
    publishOutcome(order, "TransferCancelled");
  }

  /** 정산 재시도 예약. 시도 횟수를 누적해 무한 재조회를 막는다. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markUnknownRetry(
      Long orderId, String reason, LocalDateTime nextReconcileAt, int attempts) {
    TransferOrderEntity order = load(orderId);
    order.setStatus(TransferOrderEntity.Status.UNKNOWN);
    order.setFailedReason(truncate(reason));
    order.setNextReconcileAt(nextReconcileAt);
    order.setReconcileAttempts(attempts);
  }

  private void publishOutcome(TransferOrderEntity order, String eventType) {
    // 상태 변경과 같은 트랜잭션에서 아웃박스에 적재한다 (dual-write 방지).
    outboxPublisher.publish(
        "TransferOrder",
        String.valueOf(order.getOrderId()),
        eventType,
        Map.of(
            "orderId", order.getOrderId(),
            "userId", String.valueOf(order.getUser().getUserId()),
            "amount", order.getAmount(),
            "toAccountMasked", order.getToAccountNoMasked(),
            "status", order.getStatus().name(),
            "bankTransactionId",
                order.getBankTransactionId() == null ? "" : order.getBankTransactionId()));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Optional<TransferOrderEntity> find(Long orderId) {
    return transferOrderRepository.findById(orderId);
  }

  private TransferOrderEntity load(Long orderId) {
    return transferOrderRepository
        .findById(orderId)
        .orElseThrow(
            () ->
                new BusinessException(
                    ErrorCode.TRANSFER_NOT_FOUND, AppMessages.Transfer.NOT_FOUND));
  }

  private static String truncate(String value) {
    if (value == null) {
      return null;
    }
    return value.length() <= 255 ? value : value.substring(0, 255);
  }
}
