package com.nuri.adapter.out.store;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(prefix = "nuri.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryTokenStore implements TokenStore {
    private final Map<String, String> store = new ConcurrentHashMap<>();

    @Override
    public void put(String key, String value) { store.put(key, value); }

    @Override
    public String get(String key) { return store.get(key); }
}
