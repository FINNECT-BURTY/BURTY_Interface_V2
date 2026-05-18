package com.burty.security.oauth;

import com.burty.domain.entity.OAuthStateEntity;
import com.burty.domain.repository.OAuthStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DB 영속 OAuth state store. JVM 재시작/멀티 인스턴스 환경에 안전.
 *
 * - state_key 는 SHA-256(provider:state) → PK 길이/인덱스 효율 + 평문 state 미저장
 * - verifyAndConsume 은 단발성: 검증 성공 시 즉시 삭제 (replay 방지)
 * - cleanup: 모든 verify/remember 사이클에서 일정 빈도로 만료 row 일괄 삭제 (별도 스케줄러 불필요)
 */
@Primary
@Component
public class JpaOAuthStateStore implements OAuthStateStore {

    private static final Logger log = LoggerFactory.getLogger(JpaOAuthStateStore.class);

    private final Clock clock = Clock.system(ZoneId.systemDefault());
    private final Duration ttl = Duration.ofMinutes(10);
    private final OAuthStateRepository repository;
    private final AtomicLong opsSinceCleanup = new AtomicLong();
    private static final long CLEANUP_EVERY_N_OPS = 50;

    public JpaOAuthStateStore(OAuthStateRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void remember(String provider, String state, String frontendOrigin) {
        if (state == null || state.isBlank()) {
            return;
        }
        String key = stateKey(provider, state);
        OAuthStateEntity entity = new OAuthStateEntity();
        entity.setStateKey(key);
        entity.setProvider(provider == null ? "" : provider.toUpperCase());
        entity.setFrontendOrigin(blank(frontendOrigin) ? null : frontendOrigin.trim());
        entity.setExpiresAt(now().plus(ttl));
        try {
            repository.save(entity);
        } catch (Exception e) {
            // PK 충돌 (동일 state 재발급) 등은 무시 — 어차피 같은 의미
            log.debug("OAuthStateStore remember: skip (likely duplicate) provider={} reason={}", provider, e.getClass().getSimpleName());
        }
        maybeCleanup();
    }

    @Override
    @Transactional
    public OAuthStateContext verifyAndConsume(String provider, String state) {
        if (state == null || state.isBlank()) {
            throw new IllegalStateException("OAuth state가 필요합니다.");
        }
        String key = stateKey(provider, state);
        Optional<OAuthStateEntity> maybe = repository.findById(key);
        if (maybe.isEmpty()) {
            throw new IllegalStateException("OAuth state가 유효하지 않거나 만료되었습니다.");
        }
        OAuthStateEntity entity = maybe.get();
        String frontendOrigin = entity.getFrontendOrigin();
        // 검증 결과와 무관하게 단발성 — 삭제 먼저 (replay 방지)
        try {
            repository.deleteById(key);
        } catch (Exception e) {
            log.warn("OAuthStateStore: delete after verify failed key={}", key, e);
        }
        if (entity.getExpiresAt().isBefore(now())) {
            throw new IllegalStateException("OAuth state가 유효하지 않거나 만료되었습니다.");
        }
        maybeCleanup();
        return new OAuthStateContext(frontendOrigin);
    }

    private void maybeCleanup() {
        long count = opsSinceCleanup.incrementAndGet();
        if (count % CLEANUP_EVERY_N_OPS != 0) return;
        try {
            int removed = repository.deleteExpired(now());
            if (removed > 0) {
                log.debug("OAuthStateStore cleanup removed={} expired rows", removed);
            }
        } catch (Exception e) {
            log.warn("OAuthStateStore cleanup failed", e);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(Instant.now(clock), ZoneId.systemDefault());
    }

    private static String stateKey(String provider, String state) {
        try {
            String raw = (provider == null ? "" : provider.toUpperCase()) + ":" + state;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("OAuth state key hashing failed", e);
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
