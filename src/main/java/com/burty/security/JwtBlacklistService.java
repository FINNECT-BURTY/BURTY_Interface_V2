package com.burty.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JWT 블랙리스트. Redis 가 있으면 Redis 로, 없으면 in-memory map 으로 fallback.
 *
 * Redis 미설치 환경(현재 운영)에서 매 요청마다 WARN 로그가 찍히는 noise 를 피하기 위해
 * 처음 1회만 경고하고 그 이후는 silent 로 fallback 사용.
 */
@Service
@Slf4j
public class JwtBlacklistService {
    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final ConcurrentHashMap<String, Long> localBlacklist = new ConcurrentHashMap<>();
    private final AtomicBoolean redisFallbackWarned = new AtomicBoolean(false);

    public JwtBlacklistService(StringRedisTemplate redisTemplate, JwtTokenProvider jwtTokenProvider) {
        this.redisTemplate = redisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public void blacklist(String token) {
        long ttl = jwtTokenProvider.getRemainingTtlSeconds(token);
        try {
            redisTemplate.opsForValue().set("burty:jwt:blacklist:" + token, "1", ttl, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            warnRedisFallbackOnce();
            localBlacklist.put(token, System.currentTimeMillis() + (ttl * 1000));
        }
    }

    public boolean isBlacklisted(String token) {
        try {
            Boolean exists = redisTemplate.hasKey("burty:jwt:blacklist:" + token);
            return Boolean.TRUE.equals(exists);
        } catch (Exception ignored) {
            warnRedisFallbackOnce();
            Long expiresAt = localBlacklist.get(token);
            if (expiresAt == null) return false;
            if (expiresAt < System.currentTimeMillis()) {
                localBlacklist.remove(token);
                return false;
            }
            return true;
        }
    }

    private void warnRedisFallbackOnce() {
        if (redisFallbackWarned.compareAndSet(false, true)) {
            log.warn("Redis unavailable for JWT blacklist — using local in-memory map fallback. " +
                    "OK for single-instance; multi-instance 환경에서는 Redis 활성화 필요.");
        }
    }
}
