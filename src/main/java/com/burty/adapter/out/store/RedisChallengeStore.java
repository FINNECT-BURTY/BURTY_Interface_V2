/**
 *
 *
 * <pre>
 * <b>Description  : 외부연동 (RedisChallengeStore)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.out.store
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.adapter.out.store;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "burty.redis", name = "enabled", havingValue = "true")
public class RedisChallengeStore implements ChallengeStore {
  private final StringRedisTemplate redisTemplate;

  public RedisChallengeStore(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public void put(String key, String value, long ttlSeconds) {
    redisTemplate
        .opsForValue()
        .set("burty:challenge:" + key, value, Duration.ofSeconds(ttlSeconds));
  }

  @Override
  public String get(String key) {
    return redisTemplate.opsForValue().get("burty:challenge:" + key);
  }

  @Override
  public void remove(String key) {
    redisTemplate.delete("burty:challenge:" + key);
  }
}
