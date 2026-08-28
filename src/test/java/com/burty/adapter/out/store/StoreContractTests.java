package com.burty.adapter.out.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.burty.support.RedisTestContainer;
import com.burty.util.FieldEncryptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 저장소 어댑터 계약.
 *
 * <p>{@code Redis*} 구현은 {@code burty.redis.enabled=true} 일 때만 만들어진다. 테스트는 전부 {@code false} 라, 지금까지
 * 검증된 것은 in-memory 구현뿐이고 <b>운영에서 도는 쪽은 한 번도 실행된 적이 없었다.</b> 캐시 직렬화 결함이 정확히 그렇게 운영에서만 드러났다.
 *
 * <p>그래서 같은 계약을 두 구현에 나란히 건다. Docker 가 없으면 Redis 쪽은 건너뛴다.
 */
class StoreContractTests {

  private static String key() {
    return "contract-" + UUID.randomUUID();
  }

  private static StringRedisTemplate redis() {
    StringRedisTemplate template = RedisTestContainer.template();
    assumeTrue(template != null, "Docker 가 없어 Redis 구현 검증을 건너뛴다");
    return template;
  }

  @Nested
  @DisplayName("레이트리밋 저장소")
  class RateLimit {

    static List<org.junit.jupiter.params.provider.Arguments> implementations() {
      return List.of(
          org.junit.jupiter.params.provider.Arguments.of(
              "in-memory", (Supplier<RateLimitStore>) InMemoryRateLimitStore::new),
          org.junit.jupiter.params.provider.Arguments.of(
              "redis", (Supplier<RateLimitStore>) () -> new RedisRateLimitStore(redis())));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("한도까지는 통과시키고 넘으면 막는다")
    void allowsUpToLimit(String name, Supplier<RateLimitStore> factory) {
      RateLimitStore store = factory.get();
      String key = key();

      for (int i = 1; i <= 3; i++) {
        assertTrue(store.tryConsume(key, 3, 60_000L), i + "번째 요청이 막혔다");
      }
      assertFalse(store.tryConsume(key, 3, 60_000L), "한도를 넘은 요청이 통과했다");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("키가 다르면 서로 영향을 주지 않는다")
    void keysAreIndependent(String name, Supplier<RateLimitStore> factory) {
      RateLimitStore store = factory.get();
      String a = key();
      String b = key();

      assertTrue(store.tryConsume(a, 1, 60_000L));
      assertFalse(store.tryConsume(a, 1, 60_000L));
      assertTrue(store.tryConsume(b, 1, 60_000L), "다른 키가 함께 막혔다");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("창이 지나면 다시 통과시킨다")
    void windowResets(String name, Supplier<RateLimitStore> factory) throws Exception {
      RateLimitStore store = factory.get();
      String key = key();

      assertTrue(store.tryConsume(key, 1, 300L));
      assertFalse(store.tryConsume(key, 1, 300L));
      Thread.sleep(400L);
      // 창이 지나도 풀리지 않으면 그 주체는 영구 차단된다. Redis 구현에서 실제로 그럴 수 있었다 —
      // INCR 후 EXPIRE 를 따로 걸어서, 그 사이에 연결이 끊기면 만료가 영영 걸리지 않았다.
      assertTrue(store.tryConsume(key, 1, 300L), "창이 지났는데도 계속 막힌다");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("만료가 걸리지 않은 키를 만나면 스스로 회복한다")
    void recoversFromKeyWithoutTtl(String name, Supplier<RateLimitStore> factory) throws Exception {
      RateLimitStore store = factory.get();
      String key = key();
      assertTrue(store.tryConsume(key, 1, 300L));

      // 과거 사고로 TTL 없이 남은 키를 흉내낸다.
      if (name.equals("redis")) {
        redis().persist("burty:ratelimit:" + key);
      }

      Thread.sleep(400L);
      assertTrue(store.tryConsume(key, 1, 300L), "TTL 이 없는 키에서 회복하지 못했다");
    }
  }

  @Nested
  @DisplayName("멱등성 저장소")
  class Idempotency {

    static List<org.junit.jupiter.params.provider.Arguments> implementations() {
      ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
      return List.of(
          org.junit.jupiter.params.provider.Arguments.of(
              "in-memory", (Supplier<IdempotencyStore>) InMemoryIdempotencyStore::new),
          org.junit.jupiter.params.provider.Arguments.of(
              "redis",
              (Supplier<IdempotencyStore>) () -> new RedisIdempotencyStore(redis(), mapper)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("저장한 응답을 그대로 돌려준다")
    void storesAndReturnsValue(String name, Supplier<IdempotencyStore> factory) {
      IdempotencyStore store = factory.get();
      String key = key();

      store.put(key, Map.of("orderId", "ORD-1", "amount", 1000), 60L);

      Optional<Map<String, Object>> found = store.get(key);
      assertTrue(found.isPresent(), "저장한 응답을 찾지 못했다");
      assertEquals("ORD-1", found.get().get("orderId"));
      assertEquals(1000, ((Number) found.get().get("amount")).intValue());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("없는 키는 빈 값이다")
    void missingKeyIsEmpty(String name, Supplier<IdempotencyStore> factory) {
      assertTrue(factory.get().get(key()).isEmpty());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("TTL 이 지나면 사라진다")
    void expires(String name, Supplier<IdempotencyStore> factory) throws Exception {
      IdempotencyStore store = factory.get();
      String key = key();
      store.put(key, Map.of("orderId", "ORD-2"), 1L);

      Thread.sleep(1_200L);

      assertTrue(store.get(key).isEmpty(), "TTL 이 지났는데도 남아 있다");
    }
  }

  @Nested
  @DisplayName("챌린지 저장소")
  class Challenge {

    static List<org.junit.jupiter.params.provider.Arguments> implementations() {
      return List.of(
          org.junit.jupiter.params.provider.Arguments.of(
              "in-memory", (Supplier<ChallengeStore>) InMemoryChallengeStore::new),
          org.junit.jupiter.params.provider.Arguments.of(
              "redis", (Supplier<ChallengeStore>) () -> new RedisChallengeStore(redis())));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("저장·조회·삭제")
    void putGetRemove(String name, Supplier<ChallengeStore> factory) {
      ChallengeStore store = factory.get();
      String key = key();

      store.put(key, "user-1|AUTHENTICATION", 60L);
      assertEquals("user-1|AUTHENTICATION", store.get(key));

      store.remove(key);
      // 챌린지가 지워지지 않으면 재사용할 수 있다.
      assertNull(store.get(key), "삭제한 챌린지가 남아 있다");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("TTL 이 지나면 사라진다")
    void expires(String name, Supplier<ChallengeStore> factory) throws Exception {
      ChallengeStore store = factory.get();
      String key = key();
      store.put(key, "user-1|AUTHENTICATION", 1L);

      Thread.sleep(1_200L);

      assertNull(store.get(key), "TTL 이 지났는데도 남아 있다");
    }
  }

  @Nested
  @DisplayName("토큰 저장소")
  class Token {

    private static FieldEncryptor encryptor() {
      return new FieldEncryptor("burty-store-contract-test-key-0001", 2, null, 0);
    }

    static List<org.junit.jupiter.params.provider.Arguments> implementations() {
      return List.of(
          org.junit.jupiter.params.provider.Arguments.of(
              "in-memory", (Supplier<TokenStore>) () -> new InMemoryTokenStore(encryptor())),
          org.junit.jupiter.params.provider.Arguments.of(
              "redis", (Supplier<TokenStore>) () -> new RedisTokenStore(redis(), encryptor())));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("저장한 토큰을 복호화해 돌려준다")
    void roundTrips(String name, Supplier<TokenStore> factory) {
      TokenStore store = factory.get();
      String key = key();

      store.put(key, "access-token-value");

      assertEquals("access-token-value", store.get(key));
      store.remove(key);
      assertNull(store.get(key));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("저장된 값은 평문이 아니다")
    void storedValueIsEncrypted(String name, Supplier<TokenStore> factory) {
      assumeTrue(name.equals("redis"), "저장 매체를 직접 볼 수 있는 Redis 구현에서만 확인한다");
      TokenStore store = factory.get();
      String key = key();

      store.put(key, "access-token-value");

      String raw = redis().opsForValue().get("burty:token:" + key);
      assertFalse(raw != null && raw.contains("access-token-value"), "토큰이 평문으로 저장됐다: " + raw);
    }
  }
}
