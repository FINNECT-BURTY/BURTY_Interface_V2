package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.burty.domain.auth.repository.OAuthStateRepository;
import com.burty.security.oauth.OAuthStateStore;
import com.burty.support.IntegrationTestBase;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * OAuth state 의 일회용 보장.
 *
 * <p>state 는 소셜 로그인 콜백의 CSRF 방어다. <b>한 번만</b> 쓸 수 있어야 의미가 있는데, 예전 구현은 조회한 뒤 삭제하는 방식이라 그 보장이 없었다.
 *
 * <ul>
 *   <li>같은 state 로 두 콜백이 동시에 들어오면 둘 다 조회에 성공해 둘 다 통과한다.
 *   <li>삭제가 실패하면 {@code warn} 으로 삼키고 그대로 성공 처리했다. 지워지지 않은 state 로 몇 번이든 다시 들어올 수 있었다.
 * </ul>
 *
 * <p><b>주의</b> — 순차 호출만으로는 수정 전후가 구분되지 않는다. 삭제가 성공하는 정상 경로에서는 예전 구현도 두 번째 호출을 거절하기 때문이다. 그래서 일회용
 * 보장이 기대는 것({@code consume} 이 한 번만 1 을 돌려준다)을 별도 트랜잭션으로 따로 확인한다. 나머지는 회귀 감지용이다.
 */
@SpringBootTest
class OAuthStateSingleUseTests extends IntegrationTestBase {

  private static final String PROVIDER = "KAKAO";

  @Autowired private OAuthStateStore stateStore;
  @Autowired private OAuthStateRepository stateRepository;
  @Autowired private TransactionTemplate transactionTemplate;

  @Test
  @DisplayName("한 번 소비한 state 는 다시 쓸 수 없다")
  void stateCannotBeConsumedTwice() {
    String state = newState();
    stateStore.remember(PROVIDER, state, "https://burty.example");

    assertNotNull(stateStore.verifyAndConsume(PROVIDER, state));

    // 두 번째 호출이 통과하면 콜백을 그대로 재생할 수 있다.
    assertThrows(IllegalStateException.class, () -> stateStore.verifyAndConsume(PROVIDER, state));
    // 소비한 state 는 남아 있으면 안 된다. 예전 구현은 삭제 실패를 warn 으로 삼키고
    // 그대로 성공 처리했기 때문에, 지워지지 않은 채 몇 번이든 다시 들어올 수 있었다.
    assertEquals(0, stateRepository.count(), "소비한 state 가 남아 있다");
  }

  @Test
  @DisplayName("선점은 한 번만 성공한다")
  void consumeClaimsExactlyOnce() {
    String state = newState();
    stateStore.remember(PROVIDER, state, "https://burty.example");
    String key = stateKeyOf(state);

    // 일회용 보장이 기대는 것은 이 한 줄이다. 먼저 도착한 쪽만 1 을 받아야
    // 동시에 들어온 콜백 중 하나만 통과한다. 요청 하나당 트랜잭션 하나로 확인한다.
    assertEquals(1, consumeInOwnTransaction(key));
    assertEquals(0, consumeInOwnTransaction(key));
  }

  @Test
  @DisplayName("저장한 적 없는 state 는 거절한다")
  void unknownStateIsRejected() {
    assertThrows(
        IllegalStateException.class, () -> stateStore.verifyAndConsume(PROVIDER, newState()));
  }

  private int consumeInOwnTransaction(String key) {
    return transactionTemplate.execute(status -> stateRepository.consume(key));
  }

  /**
   * 저장소가 쓰는 키를 그대로 계산한다.
   *
   * <p>구현이 원문 state 를 저장하지 않고 해시로 바꿔 넣기 때문에, 저장 직후 남은 행에서 키를 읽어 온다.
   */
  private String stateKeyOf(String state) {
    return stateRepository.findAll().stream()
        .reduce((first, second) -> second)
        .orElseThrow()
        .getStateKey();
  }

  private static String newState() {
    return "state-" + UUID.randomUUID();
  }
}
