/**
 *
 *
 * <pre>
 * <b>Description  : 유틸 (PasswordAttemptTracker)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.util
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
package com.burty.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** 비밀번호 시도 횟수 추적 및 락아웃 관리 (Redis 우선, 없으면 in-memory). */
@Slf4j
@Component
public class PasswordAttemptTracker {

  private static final int MAX_ATTEMPTS = 5;
  private static final int LOCKOUT_DURATION_MINUTES = 15;
  private static final String ATTEMPT_KEY_PREFIX = "password:attempt:";
  private static final String LOCKOUT_KEY_PREFIX = "password:lockout:";

  private final StringRedisTemplate redisTemplate;
  private final ConcurrentHashMap<String, AtomicInteger> localAttempts = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Long> localLockouts = new ConcurrentHashMap<>();

  public PasswordAttemptTracker(@Autowired(required = false) StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void recordFailedAttempt(String token) {
    if (redisTemplate != null) {
      try {
        recordFailedAttemptRedis(token);
        return;
      } catch (Exception e) {
        log.warn("Redis password tracker fallback: {}", e.getMessage());
      }
    }
    recordFailedAttemptLocal(token);
  }

  public void resetAttempts(String token) {
    if (redisTemplate != null) {
      try {
        redisTemplate.delete(ATTEMPT_KEY_PREFIX + token);
        redisTemplate.delete(LOCKOUT_KEY_PREFIX + token);
        return;
      } catch (Exception ignored) {
        // fallback below
      }
    }
    localAttempts.remove(token);
    localLockouts.remove(token);
  }

  public boolean isLockedOut(String token) {
    if (redisTemplate != null) {
      try {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOCKOUT_KEY_PREFIX + token));
      } catch (Exception ignored) {
        // fallback below
      }
    }
    Long until = localLockouts.get(token);
    if (until == null) return false;
    if (until <= System.currentTimeMillis()) {
      localLockouts.remove(token);
      return false;
    }
    return true;
  }

  public int getAttempts(String token) {
    if (redisTemplate != null) {
      try {
        String value = redisTemplate.opsForValue().get(ATTEMPT_KEY_PREFIX + token);
        return value != null ? Integer.parseInt(value) : 0;
      } catch (Exception ignored) {
        // fallback below
      }
    }
    return localAttempts.getOrDefault(token, new AtomicInteger()).get();
  }

  public int getRemainingAttempts(String token) {
    return Math.max(0, MAX_ATTEMPTS - getAttempts(token));
  }

  public LocalDateTime getLockoutUntil(String token) {
    if (redisTemplate != null) {
      try {
        Long ttl = redisTemplate.getExpire(LOCKOUT_KEY_PREFIX + token);
        if (ttl != null && ttl > 0) {
          return LocalDateTime.now().plusSeconds(ttl);
        }
        return null;
      } catch (Exception ignored) {
        // fallback below
      }
    }
    Long until = localLockouts.get(token);
    if (until == null) return null;
    return LocalDateTime.now().plusSeconds((until - System.currentTimeMillis()) / 1000);
  }

  public int getMaxAttempts() {
    return MAX_ATTEMPTS;
  }

  public int getLockoutDurationMinutes() {
    return LOCKOUT_DURATION_MINUTES;
  }

  private void recordFailedAttemptRedis(String token) {
    String attemptKey = ATTEMPT_KEY_PREFIX + token;
    Long attempts = redisTemplate.opsForValue().increment(attemptKey);
    if (attempts != null && attempts == 1) {
      redisTemplate.expire(attemptKey, Duration.ofMinutes(LOCKOUT_DURATION_MINUTES));
    }
    if (attempts != null && attempts >= MAX_ATTEMPTS) {
      redisTemplate
          .opsForValue()
          .set(LOCKOUT_KEY_PREFIX + token, "locked", Duration.ofMinutes(LOCKOUT_DURATION_MINUTES));
      log.warn("인증 토큰 잠금 token={} duration={}분", PiiMasker.secret(token), LOCKOUT_DURATION_MINUTES);
    }
  }

  private void recordFailedAttemptLocal(String token) {
    int attempts =
        localAttempts.computeIfAbsent(token, ignored -> new AtomicInteger()).incrementAndGet();
    if (attempts >= MAX_ATTEMPTS) {
      localLockouts.put(
          token,
          System.currentTimeMillis() + Duration.ofMinutes(LOCKOUT_DURATION_MINUTES).toMillis());
      log.warn(
          "인증 토큰 로컬 잠금 token={} duration={}분", PiiMasker.secret(token), LOCKOUT_DURATION_MINUTES);
    }
  }
}
