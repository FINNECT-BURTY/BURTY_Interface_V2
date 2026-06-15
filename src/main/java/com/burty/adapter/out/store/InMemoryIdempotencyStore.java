package com.burty.adapter.out.store;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "burty.redis",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class InMemoryIdempotencyStore implements IdempotencyStore {

  private final Map<String, Entry> store = new ConcurrentHashMap<>();

  @Override
  public Optional<Map<String, Object>> get(String key) {
    Entry entry = store.get(key);
    if (entry == null) {
      return Optional.empty();
    }
    if (entry.expireAt.isBefore(LocalDateTime.now())) {
      store.remove(key);
      return Optional.empty();
    }
    return Optional.of(entry.value);
  }

  @Override
  public void put(String key, Map<String, Object> value, long ttlSeconds) {
    store.put(key, new Entry(value, LocalDateTime.now().plusSeconds(ttlSeconds)));
  }

  private record Entry(Map<String, Object> value, LocalDateTime expireAt) {}
}
