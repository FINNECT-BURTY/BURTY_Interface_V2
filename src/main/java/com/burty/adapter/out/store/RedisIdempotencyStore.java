package com.burty.adapter.out.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "burty.redis", name = "enabled", havingValue = "true")
public class RedisIdempotencyStore implements IdempotencyStore {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  public RedisIdempotencyStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
  }

  @Override
  public Optional<Map<String, Object>> get(String key) {
    String raw = redisTemplate.opsForValue().get("burty:idempotency:" + key);
    if (raw == null || raw.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(objectMapper.readValue(raw, MAP_TYPE));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public void put(String key, Map<String, Object> value, long ttlSeconds) {
    try {
      String json = objectMapper.writeValueAsString(value);
      redisTemplate
          .opsForValue()
          .set("burty:idempotency:" + key, json, ttlSeconds, TimeUnit.SECONDS);
    } catch (Exception ignored) {
      // 멱등성 저장 실패 시 이체 응답은 그대로 반환
    }
  }
}
