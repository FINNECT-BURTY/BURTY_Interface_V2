/**
 *
 *
 * <pre>
 * <b>Description  : 보안 애플리케이션 서비스 (JwtBlacklistService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.security
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
package com.burty.security;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** JWT 블랙리스트. Redis 가 있으면 Redis 로, 없으면 in-memory map 으로 fallback. */
@Service
@Slf4j
public class JwtBlacklistService {
  private final StringRedisTemplate redisTemplate;
  private final JwtTokenProvider jwtTokenProvider;
  private final ConcurrentHashMap<String, Long> localBlacklist = new ConcurrentHashMap<>();
  private final AtomicBoolean redisFallbackWarned = new AtomicBoolean(false);

  public JwtBlacklistService(
      @Autowired(required = false) StringRedisTemplate redisTemplate,
      JwtTokenProvider jwtTokenProvider) {
    this.redisTemplate = redisTemplate;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  public void blacklist(String token) {
    long ttl = jwtTokenProvider.getRemainingTtlSeconds(token);
    if (redisTemplate != null) {
      try {
        redisTemplate
            .opsForValue()
            .set("burty:jwt:blacklist:" + token, "1", Duration.ofSeconds(ttl));
        return;
      } catch (Exception ignored) {
        warnRedisFallbackOnce();
      }
    }
    localBlacklist.put(token, System.currentTimeMillis() + (ttl * 1000));
  }

  public boolean isBlacklisted(String token) {
    if (redisTemplate != null) {
      try {
        Boolean exists = redisTemplate.hasKey("burty:jwt:blacklist:" + token);
        return Boolean.TRUE.equals(exists);
      } catch (Exception ignored) {
        warnRedisFallbackOnce();
      }
    }
    Long expiresAt = localBlacklist.get(token);
    if (expiresAt == null) return false;
    if (expiresAt < System.currentTimeMillis()) {
      localBlacklist.remove(token);
      return false;
    }
    return true;
  }

  private void warnRedisFallbackOnce() {
    if (redisFallbackWarned.compareAndSet(false, true)) {
      log.warn(
          "Redis unavailable for JWT blacklist — using local in-memory map fallback. "
              + "OK for single-instance; multi-instance 환경에서는 Redis 활성화 필요.");
    }
  }
}
