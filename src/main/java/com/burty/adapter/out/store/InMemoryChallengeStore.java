package com.burty.adapter.out.store;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(prefix = "burty.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryChallengeStore implements ChallengeStore {
    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public void put(String key, String value, long ttlSeconds) {
        store.put(key, new Entry(value, LocalDateTime.now().plusSeconds(ttlSeconds)));
    }

    @Override
    public String get(String key) {
        Entry entry = store.get(key);
        if (entry == null) return null;
        if (entry.expireAt.isBefore(LocalDateTime.now())) {
            store.remove(key);
            return null;
        }
        return entry.value;
    }

    @Override
    public void remove(String key) { store.remove(key); }

    private static class Entry {
        private final String value;
        private final LocalDateTime expireAt;
        private Entry(String value, LocalDateTime expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }
}
