package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.application.service.finance.TransferLimitGuard;
import com.burty.core.exception.BusinessException;
import com.burty.domain.user.entity.UserSettingEntity;
import com.burty.domain.user.repository.UserSettingRepository;
import com.burty.support.IntegrationTestBase;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 일일 이체 한도의 동시성 검증.
 *
 * <p>예전 구현은 "조회 → 검사 → 나중에 증가" 였고 락이 없었다. 동시 요청 N 건이 모두 같은 잔여한도를 읽고 통과해서 한도의 N 배까지 이체가 됐다. 이 테스트는 그
 * 회귀를 막는다.
 */
@SpringBootTest
class TransferLimitGuardConcurrencyTests extends IntegrationTestBase {

  @Autowired private TransferLimitGuard limitGuard;
  @Autowired private UserSettingRepository userSettingRepository;

  @Test
  @DisplayName("동시 예약 요청이 몰려도 일일 한도를 넘지 못한다")
  void concurrentReservationsNeverExceedDailyLimit() throws Exception {
    String userId = newUserId();
    long limit = 10_000L;
    long amountEach = 1_000L;
    int threads = 40;
    setDailyLimit(userId, limit);

    AtomicInteger accepted = new AtomicInteger();
    AtomicInteger rejected = new AtomicInteger();

    try (ExecutorService pool = Executors.newFixedThreadPool(16)) {
      List<Callable<Void>> tasks =
          java.util.stream.IntStream.range(0, threads)
              .<Callable<Void>>mapToObj(
                  i ->
                      () -> {
                        try {
                          limitGuard.reserve(userId, amountEach);
                          accepted.incrementAndGet();
                        } catch (BusinessException e) {
                          rejected.incrementAndGet();
                        }
                        return null;
                      })
              .toList();
      for (Future<Void> future : pool.invokeAll(tasks)) {
        future.get();
      }
    }

    // 한도 10,000 / 건당 1,000 → 정확히 10건만 통과해야 한다.
    assertEquals(limit / amountEach, accepted.get(), "허용된 건수가 한도와 일치하지 않습니다");
    assertEquals(threads - limit / amountEach, rejected.get());
    assertEquals(limit, limitGuard.currentUsage(userId), "누적 사용액이 한도를 초과했습니다");
  }

  @Test
  @DisplayName("1회 이체 한도를 넘는 요청은 일일 한도와 무관하게 거절된다")
  void perTransactionLimitIsEnforced() {
    String userId = newUserId();
    setDailyLimit(userId, 100_000_000L);
    setSetting(userId, "TRANSFER_PER_TX_LIMIT", 50_000L);

    assertThrows(BusinessException.class, () -> limitGuard.reserve(userId, 50_001L));
    assertEquals(0L, limitGuard.currentUsage(userId));
  }

  @Test
  @DisplayName("확정 실패한 이체의 예약은 해제되어 한도가 복구된다")
  void releaseRestoresAvailableLimit() {
    String userId = newUserId();
    setDailyLimit(userId, 5_000L);

    limitGuard.reserve(userId, 5_000L);
    assertThrows(BusinessException.class, () -> limitGuard.reserve(userId, 1L));

    limitGuard.release(userId, 5_000L, java.time.LocalDate.now());
    assertEquals(0L, limitGuard.currentUsage(userId));
    limitGuard.reserve(userId, 5_000L);
    assertTrue(limitGuard.currentUsage(userId) == 5_000L);
  }

  @Test
  @DisplayName("설정이 없으면 기본 한도가 적용된다")
  void defaultLimitsApplyWhenUnconfigured() {
    String userId = newUserId();
    assertEquals(5_000_000L, limitGuard.resolveDailyLimit(userId));
    assertEquals(3_000_000L, limitGuard.resolvePerTransactionLimit(userId));
    assertEquals(0L, limitGuard.currentUsage(userId));
  }

  @Test
  @DisplayName("사용자별 한도 설정이 기본값을 덮어쓴다")
  void configuredLimitOverridesDefault() {
    String userId = newUserId();
    setDailyLimit(userId, 123_000L);
    assertEquals(123_000L, limitGuard.resolveDailyLimit(userId));
  }

  @Test
  @DisplayName("예약 후 사용량이 정확히 누적된다")
  void usageAccumulatesAcrossReservations() {
    String userId = newUserId();
    setDailyLimit(userId, 10_000L);

    limitGuard.reserve(userId, 3_000L);
    limitGuard.reserve(userId, 2_000L);

    assertEquals(5_000L, limitGuard.currentUsage(userId));
  }

  @Test
  @DisplayName("보유 사용량보다 큰 금액 해제는 무시된다 (음수 방지)")
  void releasingMoreThanReservedIsIgnored() {
    String userId = newUserId();
    setDailyLimit(userId, 10_000L);
    limitGuard.reserve(userId, 1_000L);

    limitGuard.release(userId, 9_999L, java.time.LocalDate.now());

    assertEquals(1_000L, limitGuard.currentUsage(userId), "사용량이 음수가 되면 안 된다");
  }

  private static final AtomicInteger SEQ = new AtomicInteger(900_000);

  private String newUserId() {
    // 사용량 테이블은 user_id 를 FK 없이 쓰므로 임의의 유일한 값이면 충분하다.
    return String.valueOf(SEQ.incrementAndGet());
  }

  private void setDailyLimit(String userId, long limit) {
    setSetting(userId, "TRANSFER_LIMIT", limit);
  }

  private void setSetting(String userId, String key, long value) {
    UserSettingEntity setting =
        userSettingRepository
            .findByUserIdAndSettingKey(userId, key)
            .orElseGet(UserSettingEntity::new);
    setting.setUserId(userId);
    setting.setSettingKey(key);
    setting.setSettingValueLong(value);
    setting.setSettingValueStr(String.valueOf(value));
    userSettingRepository.save(setting);
  }
}
