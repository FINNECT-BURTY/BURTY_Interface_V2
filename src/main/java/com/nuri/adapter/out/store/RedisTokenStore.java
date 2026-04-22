package com.nuri.adapter.out.store;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "nuri.redis", name = "enabled", havingValue = "true")
public class RedisTokenStore implements TokenStore {
    private final StringRedisTemplate redisTemplate;

    public RedisTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void put(String key, String value) {
        redisTemplate.opsForValue().set("nuri:token:" + key, value);
    }

    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get("nuri:token:" + key);
    }
}
