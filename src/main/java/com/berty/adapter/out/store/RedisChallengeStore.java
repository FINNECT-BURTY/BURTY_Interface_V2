package com.berty.adapter.out.store;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "berty.redis", name = "enabled", havingValue = "true")
public class RedisChallengeStore implements ChallengeStore {
    private final StringRedisTemplate redisTemplate;

    public RedisChallengeStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void put(String key, String value, long ttlSeconds) {
        redisTemplate.opsForValue().set("berty:challenge:" + key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get("berty:challenge:" + key);
    }

    @Override
    public void remove(String key) {
        redisTemplate.delete("berty:challenge:" + key);
    }
}
