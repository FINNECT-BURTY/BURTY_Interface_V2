package com.burty.adapter.out.store;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "burty.redis",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class InMemoryRateLimitStore implements RateLimitStore {

  private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

  @Override
  public boolean tryConsume(String key, int maxRequests, long windowMillis) {
    WindowCounter counter = counters.computeIfAbsent(key, ignored -> new WindowCounter());
    return counter.tryConsume(maxRequests, windowMillis);
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
