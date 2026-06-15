package com.burty.adapter.out.store;

import java.util.Map;
import java.util.Optional;

public interface IdempotencyStore {

  Optional<Map<String, Object>> get(String key);

  void put(String key, Map<String, Object> value, long ttlSeconds);
}
