package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.application.port.out.bank.OpenBankingPort;
import com.burty.application.port.out.bank.TransferStatus;
import com.burty.application.service.batch.TransferReconciliationBatch;
import com.burty.application.service.finance.TransferLimitGuard;
import com.burty.domain.finance.entity.TransferOrderEntity;
import com.burty.domain.finance.entity.TransferOrderEntity.Status;
import com.burty.domain.finance.repository.TransferOrderRepository;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.UserRepository;
import com.burty.support.IntegrationTestBase;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 이체 정산 배치 검증.
 *
 * <p>정산은 "은행 응답을 못 받은 건" 을 확정하는 유일한 경로다. 이게 없거나 잘못 동작하면 사용자는 돈이 나갔는지도 모른 채 재시도해서 이중 출금을 만든다. 특히 중요한
 * 규칙은 <b>미출금이 확인됐을 때만 한도를 되돌린다</b>는 것이다. 판단 불가 상태에서 한도를 풀어주면 실제로는 출금된 건에 대해 한도를 두 번 쓰게 된다.
 */
@SpringBootTest
class TransferReconciliationTests extends IntegrationTestBase {

  @Autowired private TransferReconciliationBatch reconciliationBatch;
  @Autowired private TransferOrderRepository transferOrderRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private TransferLimitGuard limitGuard;
  @Autowired private TransactionTemplate transactionTemplate;

  /**
   * 애플리케이션과 같은 시간대로 시각을 만들기 위해 주입받는다.
   *
   * <p>정산은 주문의 requestedAt 에서 한도 사용 날짜를 도출한다. 테스트가 시스템 기본 시간대로 requestedAt 을 쓰면 UTC 러너에서 예약된 행과 다른
   * 날짜를 가리켜 해제가 실패한다.
   */
  @Autowired private java.time.Clock clock;

  @MockitoBean private OpenBankingPort openBankingPort;

  private String userId;

  @BeforeEach
  void setUp() {
    userId = String.valueOf(createUser().getUserId());
  }

  @Test
  @DisplayName("은행이 처리 완료로 답하면 EXECUTED 로 확정하고 한도는 유지한다")
  void completedAtBankIsConfirmedAsExecuted() {
    long orderId = givenUnknownOrder(10_000L);
    limitGuard.reserve(userId, 10_000L);
    Mockito.when(openBankingPort.getTransferStatus(Mockito.eq(userId), Mockito.anyString()))
        .thenReturn(TransferStatus.completed("bank-tx-1"));

    reconciliationBatch.reconcileOnce();

    TransferOrderEntity order = reload(orderId);
    assertEquals(Status.EXECUTED, order.getStatus());
    assertEquals("bank-tx-1", order.getBankTransactionId());
    assertNotNull(order.getExecutedAt());
    assertEquals(10_000L, limitGuard.currentUsage(userId), "출금된 건의 한도를 되돌리면 안 된다");
  }

  @Test
  @DisplayName("은행에 해당 거래가 없으면 FAILED 로 확정하고 한도를 되돌린다")
  void notFoundAtBankReleasesLimit() {
    long orderId = givenUnknownOrder(10_000L);
    limitGuard.reserve(userId, 10_000L);
    Mockito.when(openBankingPort.getTransferStatus(Mockito.eq(userId), Mockito.anyString()))
        .thenReturn(TransferStatus.notFound());

    reconciliationBatch.reconcileOnce();

    assertEquals(Status.FAILED, reload(orderId).getStatus());
    assertEquals(0L, limitGuard.currentUsage(userId), "미출금이 확인됐으면 한도를 복구해야 한다");
  }

  @Test
  @DisplayName("은행이 거절로 답하면 FAILED 로 확정하고 한도를 되돌린다")
  void rejectedAtBankReleasesLimit() {
    long orderId = givenUnknownOrder(5_000L);
    limitGuard.reserve(userId, 5_000L);
    Mockito.when(openBankingPort.getTransferStatus(Mockito.eq(userId), Mockito.anyString()))
        .thenReturn(TransferStatus.rejected("잔액 부족"));

    reconciliationBatch.reconcileOnce();

    assertEquals(Status.FAILED, reload(orderId).getStatus());
    assertEquals(0L, limitGuard.currentUsage(userId));
  }

  @Test
  @DisplayName("판단이 불가하면 UNKNOWN 을 유지하고 한도도 되돌리지 않는다")
  void unresolvedKeepsUnknownAndDoesNotReleaseLimit() {
    long orderId = givenUnknownOrder(7_000L);
    limitGuard.reserve(userId, 7_000L);
    Mockito.when(openBankingPort.getTransferStatus(Mockito.eq(userId), Mockito.anyString()))
        .thenReturn(TransferStatus.unresolved("조회 실패"));

    reconciliationBatch.reconcileOnce();

    TransferOrderEntity order = reload(orderId);
    assertEquals(Status.UNKNOWN, order.getStatus(), "판단 불가를 실패로 확정하면 안 된다");
    assertEquals(1, order.getReconcileAttempts());
    assertEquals(7_000L, limitGuard.currentUsage(userId), "출금 여부를 모르는데 한도를 풀면 이중 사용이 가능해진다");
    assertTrue(order.getNextReconcileAt().isAfter(LocalDateTime.now(clock).minusMinutes(1)));
  }

  @Test
  @DisplayName("은행이 아직 처리 중이면 다음 주기로 미룬다")
  void pendingAtBankIsRescheduled() {
    long orderId = givenUnknownOrder(3_000L);
    Mockito.when(openBankingPort.getTransferStatus(Mockito.eq(userId), Mockito.anyString()))
        .thenReturn(TransferStatus.pending());

    reconciliationBatch.reconcileOnce();

    assertEquals(Status.UNKNOWN, reload(orderId).getStatus());
    assertEquals(1, reload(orderId).getReconcileAttempts());
  }

  @Test
  @DisplayName("EXECUTING 상태로 오래 멈춘 건도 정산 대상에 포함된다 (프로세스 사망 복구)")
  void stuckExecutingOrderIsReconciled() {
    long orderId =
        givenOrder(
            Status.EXECUTING,
            4_000L,
            LocalDateTime.now(clock).minusHours(1), // 충분히 오래됨
            null);
    Mockito.when(openBankingPort.getTransferStatus(Mockito.eq(userId), Mockito.anyString()))
        .thenReturn(TransferStatus.completed("bank-tx-stuck"));

    reconciliationBatch.reconcileOnce();

    assertEquals(Status.EXECUTED, reload(orderId).getStatus(), "프로세스가 죽어 멈춘 건이 방치되면 안 된다");
  }

  @Test
  @DisplayName("확정된 건은 다시 정산하지 않는다")
  void terminalOrdersAreNotReprocessed() {
    long orderId =
        givenOrder(Status.EXECUTED, 1_000L, LocalDateTime.now(clock).minusHours(2), null);

    int candidates = reconciliationBatch.reconcileOnce();

    assertEquals(0, candidates);
    Mockito.verify(openBankingPort, Mockito.never())
        .getTransferStatus(Mockito.anyString(), Mockito.anyString());
    assertEquals(Status.EXECUTED, reload(orderId).getStatus());
  }

  // ── 헬퍼 ─────────────────────────────────────────────────────────────────

  private UserEntity createUser() {
    return transactionTemplate.execute(
        status -> {
          String nonce = UUID.randomUUID().toString().replace("-", "");
          UserEntity user = new UserEntity();
          user.setCiHash(nonce + nonce);
          user.setCi("ci-" + nonce);
          user.setPhoneHash(nonce);
          user.setPhone("010-0000-0000");
          user.setCreatedAt(LocalDateTime.now(clock));
          user.setUpdatedAt(LocalDateTime.now(clock));
          return userRepository.save(user);
        });
  }

  private long givenUnknownOrder(long amount) {
    return givenOrder(
        Status.UNKNOWN, amount, LocalDateTime.now(clock), LocalDateTime.now(clock).minusMinutes(1));
  }

  private long givenOrder(
      Status status, long amount, LocalDateTime requestedAt, LocalDateTime nextReconcileAt) {
    return transactionTemplate.execute(
        txStatus -> {
          TransferOrderEntity order = new TransferOrderEntity();
          order.setUser(userRepository.findById(Long.parseLong(userId)).orElseThrow());
          order.setIdempotencyKey("recon-" + UUID.randomUUID().toString().substring(0, 20));
          order.setToAccountNo("1234567890");
          order.setToAccountNoMasked("******7890");
          order.setToBankCode("000");
          order.setAmount(amount);
          order.setStatus(status);
          order.setRequestedAt(requestedAt);
          order.setNextReconcileAt(nextReconcileAt);
          order.setReconcileAttempts(0);
          // 한도를 차감한 날짜. 해제는 이 값을 그대로 쓴다(자정 경계 대비).
          order.setLimitUsageDate(java.time.LocalDate.now(clock));
          return transferOrderRepository.save(order).getOrderId();
        });
  }

  private TransferOrderEntity reload(long orderId) {
    return transactionTemplate.execute(
        status -> transferOrderRepository.findById(orderId).orElseThrow());
  }

  @SuppressWarnings("unused")
  private LocalDate today() {
    return LocalDate.now(clock);
  }
}
