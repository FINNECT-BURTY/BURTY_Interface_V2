package com.burty.adapter.out.store;

import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "burty.redis", name = "enabled", havingValue = "true")
public class RedisRateLimitStore implements RateLimitStore {

  private final StringRedisTemplate redisTemplate;

  public RedisRateLimitStore(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public boolean tryConsume(String key, int maxRequests, long windowMillis) {
    String redisKey = "burty:ratelimit:" + key;
    Long count = redisTemplate.opsForValue().increment(redisKey);
    if (count != null && count == 1L) {
      redisTemplate.expire(redisKey, windowMillis, TimeUnit.MILLISECONDS);
    }
    return count != null && count <= maxRequests;
  }
}
