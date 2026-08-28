package com.burty.adapter.out.store;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Redis 없이 도는 환경용 레이트리밋 저장소.
 *
 * <p>인스턴스 안에서만 세므로 여러 대로 띄우면 한도가 대수만큼 늘어난다. 운영은 Redis 구현을 쓴다.
 *
 * <p>키는 요청 주체마다 하나씩 생긴다. 지우지 않으면 계속 쌓이므로 창이 한참 지난 항목은 정리한다.
 */
@Component
@ConditionalOnProperty(
    prefix = "burty.redis",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class InMemoryRateLimitStore implements RateLimitStore {

  /** 정리를 시도하는 간격. 매 요청마다 전체를 훑으면 그게 더 비싸다. */
  private static final long SWEEP_INTERVAL_MILLIS = 60_000L;

  /** 창이 끝나고 이만큼 더 지나면 버린다. */
  private static final long STALE_AFTER_WINDOWS = 2L;

  private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
  private volatile long nextSweepAt = Instant.now().toEpochMilli() + SWEEP_INTERVAL_MILLIS;

  @Override
  public boolean tryConsume(String key, int maxRequests, long windowMillis) {
    sweepIfDue(windowMillis);
    WindowCounter counter = counters.computeIfAbsent(key, ignored -> new WindowCounter());
    return counter.tryConsume(maxRequests, windowMillis);
  }

  /** 오래된 카운터를 버린다. 주체마다 키가 하나씩 생기므로 그대로 두면 무한정 늘어난다. */
  private void sweepIfDue(long windowMillis) {
    long now = Instant.now().toEpochMilli();
    if (now < nextSweepAt) {
      return;
    }
    nextSweepAt = now + SWEEP_INTERVAL_MILLIS;
    long staleBefore = now - (windowMillis * STALE_AFTER_WINDOWS);
    Iterator<Map.Entry<String, WindowCounter>> it = counters.entrySet().iterator();
    while (it.hasNext()) {
      if (it.next().getValue().windowStart < staleBefore) {
        it.remove();
      }
    }
  }

  /** 테스트·진단용 — 보관 중인 키 개수. */
  int size() {
    return counters.size();
  }

  private static final class WindowCounter {
    private volatile long windowStart = Instant.now().toEpochMilli();
    private final AtomicInteger count = new AtomicInteger();

    synchronized boolean tryConsume(int maxRequests, long windowMillis) {
      long now = Instant.now().toEpochMilli();
      if (now - windowStart >= windowMillis) {
        windowStart = now;
        count.set(0);
      }
      return count.incrementAndGet() <= maxRequests;
    }
  }
}
