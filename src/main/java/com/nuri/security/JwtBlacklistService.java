package com.nuri.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class JwtBlacklistService {
    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final ConcurrentHashMap<String, Long> localBlacklist = new ConcurrentHashMap<>();

    public JwtBlacklistService(StringRedisTemplate redisTemplate, JwtTokenProvider jwtTokenProvider) {
        this.redisTemplate = redisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public void blacklist(String token) {
        long ttl = jwtTokenProvider.getRemainingTtlSeconds(token);
        try {
            redisTemplate.opsForValue().set("nuri:jwt:blacklist:" + token, "1", ttl, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            log.warn("Redis unavailable for blacklist, fallback to local map");
            localBlacklist.put(token, System.currentTimeMillis() + (ttl * 1000));
        }
    }

    public boolean isBlacklisted(String token) {
        try {
            Boolean exists = redisTemplate.hasKey("nuri:jwt:blacklist:" + token);
            return Boolean.TRUE.equals(exists);
        } catch (Exception ignored) {
            log.warn("Redis unavailable for blacklist check, fallback to local map");
            Long expiresAt = localBlacklist.get(token);
            if (expiresAt == null) return false;
            if (expiresAt < System.currentTimeMillis()) {
                localBlacklist.remove(token);
                return false;
            }
            return true;
        }
    }
}
