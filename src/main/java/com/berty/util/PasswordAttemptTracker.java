package com.berty.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 비밀번호 시도 횟수 추적 및 락아웃 관리
 * Redis 기반 분산 환경 지원
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordAttemptTracker {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;
    private static final String ATTEMPT_KEY_PREFIX = "password:attempt:";
    private static final String LOCKOUT_KEY_PREFIX = "password:lockout:";

    private final StringRedisTemplate redisTemplate;

    // 시도 실패 기록
    public void recordFailedAttempt(String token) {
        String attemptKey = ATTEMPT_KEY_PREFIX + token;
        Long attempts = redisTemplate.opsForValue().increment(attemptKey);

        // 첫 시도일 경우 TTL 설정 (락아웃 시간 후 자동 만료)
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptKey, Duration.ofMinutes(LOCKOUT_DURATION_MINUTES));
        }

        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            String lockoutKey = LOCKOUT_KEY_PREFIX + token;
            redisTemplate.opsForValue().set(lockoutKey, "locked", Duration.ofMinutes(LOCKOUT_DURATION_MINUTES));
            log.warn("Token {} locked out for {} minutes", token, LOCKOUT_DURATION_MINUTES);
        }
    }

    // 시도 성공 시 초기화
    public void resetAttempts(String token) {
        redisTemplate.delete(ATTEMPT_KEY_PREFIX + token);
        redisTemplate.delete(LOCKOUT_KEY_PREFIX + token);
    }

    // 락아웃 상태 확인
    public boolean isLockedOut(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOCKOUT_KEY_PREFIX + token));
    }

    // 현재 시도 횟수 조회
    public int getAttempts(String token) {
        String value = redisTemplate.opsForValue().get(ATTEMPT_KEY_PREFIX + token);
        return value != null ? Integer.parseInt(value) : 0;
    }

    // 남은 시도 횟수 조회
    public int getRemainingAttempts(String token) {
        int currentAttempts = getAttempts(token);
        return Math.max(0, MAX_ATTEMPTS - currentAttempts);
    }

    // 락아웃 종료 시간 조회
    public LocalDateTime getLockoutUntil(String token) {
        Long ttl = redisTemplate.getExpire(LOCKOUT_KEY_PREFIX + token);
        if (ttl != null && ttl > 0) {
            return LocalDateTime.now().plusSeconds(ttl);
        }
        return null;
    }

    // 최대 시도 횟수 조회
    public int getMaxAttempts() {
        return MAX_ATTEMPTS;
    }

    // 락아웃 시간(분) 조회
    public int getLockoutDurationMinutes() {
        return LOCKOUT_DURATION_MINUTES;
    }
}
