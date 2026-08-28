package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.core.exception.BusinessException;
import com.burty.domain.auth.repository.UserSessionRepository;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.UserRepository;
import com.burty.security.RefreshTokenService;
import com.burty.support.IntegrationTestBase;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Refresh token 회전과 재사용 탐지.
 *
 * <p>이 경로의 방어 논리는 하나다 — <b>같은 refresh token 이 두 번 쓰이면 탈취로 보고 그 사용자의 모든 세션을 끊는다.</b> 그 전제가 두 군데서 무너져
 * 있었다.
 *
 * <ul>
 *   <li>세션을 끊은 뒤 예외를 던지는데, 같은 트랜잭션이라 예외가 끊은 것까지 되돌렸다. 응답만 "모든 세션을 종료했습니다" 이고 세션은 살아 있었다.
 *   <li>회전이 "읽어서 확인하고 쓰기" 였다. 같은 token 으로 두 요청이 동시에 들어오면 둘 다 통과해 각자 새 세션을 받았다 — 재사용 탐지가 정확히 그 상황을
 *       잡으라고 있는 것인데 그때 뚫린다.
 * </ul>
 */
@SpringBootTest
class RefreshTokenRotationTests extends IntegrationTestBase {

  @Autowired private RefreshTokenService refreshTokenService;
  @Autowired private UserSessionRepository sessionRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private TransactionTemplate transactionTemplate;

  private Long userId;

  @BeforeEach
  void setUp() {
    UserEntity user = new UserEntity();
    // ci_hash / phone_hash 는 varchar(64) 이고 각각 유니크 제약이 걸려 있다.
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
  @DisplayName("정상 회전은 새 쌍을 주고 이전 세션을 끊는다")
  void rotationIssuesNewPairAndRevokesPrevious() {
    RefreshTokenService.TokenPair first =
        refreshTokenService.issueNewSession(String.valueOf(userId), "dev-1");

    RefreshTokenService.TokenPair second = refreshTokenService.rotate(first.refreshToken());

    assertNotNull(second.refreshToken());
    assertEquals(1, activeSessionCount(), "회전 후 활성 세션은 하나여야 한다");
  }

  @Test
  @DisplayName("재사용을 감지하면 모든 세션이 실제로 끊긴다")
  void reuseDetectionActuallyRevokesSessions() {
    RefreshTokenService.TokenPair first =
        refreshTokenService.issueNewSession(String.valueOf(userId), "dev-1");
    refreshTokenService.issueNewSession(String.valueOf(userId), "dev-2");
    refreshTokenService.rotate(first.refreshToken());
    assertTrue(activeSessionCount() >= 2, "사전 조건: 활성 세션이 둘 이상이어야 한다");

    // 이미 회전시킨 token 을 다시 쓴다 = 탈취 시나리오
    assertThrows(BusinessException.class, () -> refreshTokenService.rotate(first.refreshToken()));

    // 예전에는 여기서 세션이 그대로 남아 있었다. 끊는 작업이 던진 예외에 함께 롤백됐기 때문이다.
    assertEquals(0, activeSessionCount(), "재사용을 감지했는데 세션이 살아 있다");
  }

  @Test
  @DisplayName("조건부 revoke 는 한 번만 성공한다")
  void conditionalRevokeSucceedsOnlyOnce() {
    RefreshTokenService.TokenPair issued =
        refreshTokenService.issueNewSession(String.valueOf(userId), "dev-1");
    Long sessionId = sessionRepository.findByUserIdAndRevokedAtIsNull(userId).get(0).getSessionId();

    // 회전이 기대는 것은 이 한 줄이다. 먼저 도착한 쪽만 1 을 받고 나머지는 0 을 받아야,
    // 같은 token 으로 동시에 들어온 요청 중 하나만 새 세션을 가져간다.
    // 요청 하나당 트랜잭션 하나이므로 각각 분리해 호출한다.
    assertEquals(1, revokeInOwnTransaction(sessionId));
    assertEquals(0, revokeInOwnTransaction(sessionId));
    assertNotNull(issued.refreshToken());
  }

  @Test
  @DisplayName("같은 token 으로 동시에 회전하면 한 번만 성공한다")
  void concurrentRotationSucceedsOnlyOnce() throws Exception {
    RefreshTokenService.TokenPair issued =
        refreshTokenService.issueNewSession(String.valueOf(userId), "dev-1");
    int threads = 8;

    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger succeeded = new AtomicInteger();
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    try {
      List<Future<Boolean>> futures =
          pool.invokeAll(
              java.util.Collections.nCopies(
                  threads,
                  (Callable<Boolean>)
                      () -> {
                        start.await(5, TimeUnit.SECONDS);
                        try {
                          refreshTokenService.rotate(issued.refreshToken());
                          succeeded.incrementAndGet();
                          return true;
                        } catch (RuntimeException e) {
                          return false;
                        }
                      }));
      start.countDown();
      for (Future<Boolean> future : futures) {
        future.get(30, TimeUnit.SECONDS);
      }
    } finally {
      pool.shutdownNow();
    }

    // 두 개 이상이 성공하면 탈취한 쪽도 유효한 세션을 받아간다.
    //
    // 주의: Docker 가 없으면 H2 로 강등되는데, H2 는 이 갱신을 직렬화해 경쟁이 재현되지 않는다.
    // 즉 이 테스트만으로는 수정 전 코드를 잡지 못한다 (실제로 통과했다). 회전이 기대는 의미는
    // conditionalRevokeSucceedsOnlyOnce 에서 결정적으로 확인하고, 이쪽은 회귀 감지용으로 둔다.
    assertEquals(1, succeeded.get(), "동시 회전이 여러 번 성공했다 — 재사용 탐지가 뚫린다");
  }

  private int revokeInOwnTransaction(Long sessionId) {
    return transactionTemplate.execute(
        status -> sessionRepository.revokeIfActive(sessionId, LocalDateTime.now()));
  }

  private long activeSessionCount() {
    return sessionRepository.findByUserIdAndRevokedAtIsNull(userId).size();
  }
}
