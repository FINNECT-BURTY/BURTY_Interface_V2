package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.application.service.finance.TransferOrderWriter;
import com.burty.domain.finance.entity.TransferOrderEntity;
import com.burty.domain.finance.repository.TransferOrderRepository;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.UserRepository;
import com.burty.support.IntegrationTestBase;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 이체 확정은 한 번만 일어나야 한다.
 *
 * <p>미출금으로 확정하면 한도를 되돌린다. 확정이 두 번 일어나면 <b>차감은 한 번인데 복구가 두 번</b>이 되어 그만큼 일일 한도가 늘어난다. 한도는 이 서비스가
 * 지키려는 것 자체라 그냥 넘길 수 없다.
 *
 * <p>확정 경로가 둘이다.
 *
 * <ul>
 *   <li>정산 배치 — 은행 원장을 대조해 자동 확정 ({@code @SchedulerLock} 으로 인스턴스 간에는 하나만 돈다)
 *   <li>관리자 수동 확정 — 그 락 밖에 있다
 * </ul>
 *
 * <p>예전에는 둘 다 "조회 → 상태 검사 → 저장" 이었다. 같은 주문을 동시에 집으면 둘 다 검사를 통과해 각자 확정하고 각자 한도를 되돌렸다.
 */
@SpringBootTest
class TransferSettlementOnceTests extends IntegrationTestBase {

  @Autowired private TransferOrderWriter orderWriter;
  @Autowired private TransferOrderRepository orderRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private TransactionTemplate transactionTemplate;

  private Long userId;

  @BeforeEach
  void setUp() {
    UserEntity user = new UserEntity();
    String unique = UUID.randomUUID().toString().replace("-", "");
    user.setCiHash(("c" + unique + unique).substring(0, 64));
    user.setCi("ci-" + unique);
    user.setPhoneHash(("p" + unique + unique).substring(0, 64));
    user.setPhone("010-0000-0000");
    user.setStatus(UserEntity.UserStatus.ACTIVE);
    user.setFailedLoginCount(0);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    userId = userRepository.save(user).getUserId();
  }

  @Test
  @DisplayName("미출금 확정은 한 번만 성공한다")
  void failedSettlementSucceedsOnce() {
    Long orderId = newUnknownOrder();

    assertTrue(orderWriter.settleFailed(orderId, "정산 결과: 미출금"), "첫 확정이 실패했다");
    // 두 번째가 성공하면 호출자가 한도를 한 번 더 되돌린다.
    assertFalse(orderWriter.settleFailed(orderId, "수동 확정: 미출금"), "이미 확정된 주문을 다시 확정했다");

    assertEquals(
        TransferOrderEntity.Status.FAILED,
        orderRepository.findById(orderId).orElseThrow().getStatus());
  }

  @Test
  @DisplayName("출금 완료 확정도 한 번만 성공한다")
  void executedSettlementSucceedsOnce() {
    Long orderId = newUnknownOrder();

    assertTrue(orderWriter.settleExecuted(orderId, "BANK-TXN-1"));
    assertFalse(orderWriter.settleExecuted(orderId, "BANK-TXN-2"), "이미 확정된 주문을 다시 확정했다");

    TransferOrderEntity order = orderRepository.findById(orderId).orElseThrow();
    assertEquals(TransferOrderEntity.Status.EXECUTED, order.getStatus());
    // 나중 호출이 은행 거래번호를 덮어쓰면 감사 추적이 어긋난다.
    assertEquals("BANK-TXN-1", order.getBankTransactionId());
  }

  @Test
  @DisplayName("이미 확정된 주문은 반대 결론으로도 바뀌지 않는다")
  void settledOrderCannotFlip() {
    Long orderId = newUnknownOrder();
    assertTrue(orderWriter.settleExecuted(orderId, "BANK-TXN-1"));

    // 출금된 건을 미출금으로 뒤집으면 한도가 복구된다 — 돈은 나갔는데 다시 쓸 수 있게 된다.
    assertFalse(orderWriter.settleFailed(orderId, "수동 확정: 미출금"));
    assertEquals(
        TransferOrderEntity.Status.EXECUTED,
        orderRepository.findById(orderId).orElseThrow().getStatus());
  }

  /** 은행 응답을 받지 못해 정산 대상으로 남은 주문. */
  private Long newUnknownOrder() {
    return transactionTemplate.execute(
        status -> {
          TransferOrderEntity order = new TransferOrderEntity();
          order.setUser(userRepository.findById(userId).orElseThrow());
          order.setToAccountNo("9876543210");
          order.setToAccountNoMasked("987***3210");
          order.setToBankCode("004");
          order.setToHolderName("홍길동");
          order.setAmount(10_000L);
          order.setStatus(TransferOrderEntity.Status.UNKNOWN);
          order.setIdempotencyKey("settle-" + UUID.randomUUID());
          order.setRequestedAt(LocalDateTime.now());
          order.setNextReconcileAt(LocalDateTime.now());
          return orderRepository.save(order).getOrderId();
        });
  }
}
