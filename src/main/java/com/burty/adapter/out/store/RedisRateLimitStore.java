package com.burty.adapter.out.store;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "burty.redis", name = "enabled", havingValue = "true")
public class RedisRateLimitStore implements RateLimitStore {

  /**
   * 카운트 증가와 만료 설정을 한 번에 한다.
   *
   * <p>예전에는 {@code INCR} 후 카운트가 1이면 {@code EXPIRE} 를 따로 걸었다. 두 번의 왕복 사이에 연결이 끊기거나 페일오버가 나면 INCR 만
   * 반영되고 만료가 걸리지 않는다. 그 키는 <b>영원히 만료되지 않고</b> 해당 사용자는 그 엔드포인트에서 영구 차단된다. 로그인 경로라면 계정이 잠기는 것과 같다.
   *
   * <p>스크립트는 원자적으로 실행되므로 명령이 갈라질 수 없다. 만료가 걸려 있지 않은 키를 만나면 창 정보가 없다는 뜻이므로 새 창으로 시작한다 — 과거 사고로 TTL
   * 없이 남은 키도 이때 정상화된다.
   */
  private static final RedisScript<Long> INCREMENT_WITH_TTL =
      new DefaultRedisScript<>(
          """
          local pttl = redis.call('PTTL', KEYS[1])
          if pttl < 0 then
            -- 키가 없거나(-2) 만료가 걸려 있지 않다(-1). 어느 쪽이든 창 정보가 없으므로
            -- 새 창으로 시작한다. TTL 없이 남은 키도 여기서 정상화된다.
            redis.call('SET', KEYS[1], 1, 'PX', ARGV[1])
            return 1
          end
          return redis.call('INCR', KEYS[1])
          """,
          Long.class);

  private final StringRedisTemplate redisTemplate;

  public RedisRateLimitStore(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public boolean tryConsume(String key, int maxRequests, long windowMillis) {
    Long count =
        redisTemplate.execute(
            INCREMENT_WITH_TTL, List.of("burty:ratelimit:" + key), String.valueOf(windowMillis));
    // Redis 가 응답하지 않으면 통과시킨다. 레이트리밋 장애로 서비스를 막지는 않는다.
    return count == null || count <= maxRequests;
  }
}
