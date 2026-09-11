package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.burty.adapter.out.store.ChallengeStore;
import com.burty.application.service.mydata.FinanceOAuthStateService;
import com.burty.application.service.mydata.FinanceOAuthStateService.FinanceOAuthContext;
import com.burty.core.exception.BusinessException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 인가 state 가 사용자·기관·FE 를 묶어 돌려주는지 확인한다.
 *
 * <p>콜백은 정보제공자가 브라우저를 보내 오는 것이라, 그때는 FE 가 누구인지 알 수 없다. 인가 요청 때 state 에 묶어 둔다.
 */
class FinanceOAuthStateTests {

  private final Map<String, String> store = new HashMap<>();
  private final ChallengeStore challengeStore =
      new ChallengeStore() {
        @Override
        public void put(String key, String value, long ttlSeconds) {
          store.put(key, value);
        }

        @Override
        public String get(String key) {
          return store.get(key);
        }

        @Override
        public void remove(String key) {
          store.remove(key);
        }

        @Override
        public boolean consume(String key) {
          return store.remove(key) != null;
        }
      };
  private final FinanceOAuthStateService service = new FinanceOAuthStateService(challengeStore);

  @Test
  @DisplayName("사용자·기관·FE origin 을 그대로 돌려준다")
  void roundTripsFrontendOrigin() {
    String state = service.issue("mydata", "7", "KB", "http://localhost:3000");

    FinanceOAuthContext ctx = service.consume("mydata", state);

    assertEquals("7", ctx.userId());
    assertEquals("KB", ctx.institutionCode());
    assertEquals("http://localhost:3000", ctx.frontendOrigin());
  }

  @Test
  @DisplayName("state 는 한 번만 쓸 수 있다")
  void stateIsSingleUse() {
    String state = service.issue("mydata", "7", "KB", null);
    service.consume("mydata", state);

    assertThrows(BusinessException.class, () -> service.consume("mydata", state));
  }

  @Test
  @DisplayName("FE 없이 발급한 state 는 origin 이 비어 있다")
  void issuedWithoutOrigin() {
    // 오픈뱅킹은 FE 를 묶지 않는다. 빈 문자열을 origin 으로 돌려주면 리다이렉트 주소가 깨진다.
    String state = service.issue("openbanking", "7", "OPENBANKING");

    assertNull(service.consume("openbanking", state).frontendOrigin());
  }

  @Test
  @DisplayName("배포 전에 발급된 옛 형식 state 도 읽는다")
  void readsLegacyTwoPartPayload() {
    // state 는 10분 산다. 배포 직전에 인가를 시작한 사용자의 state 는 옛 형식(userId|기관)이다.
    store.put("finance-oauth:mydata:legacy", "7|KB");

    FinanceOAuthContext ctx = service.consume("mydata", "legacy");

    assertEquals("7", ctx.userId());
    assertEquals("KB", ctx.institutionCode());
    assertNull(ctx.frontendOrigin());
  }
}
