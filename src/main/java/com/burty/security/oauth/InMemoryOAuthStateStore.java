package com.burty.security.oauth;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryOAuthStateStore implements OAuthStateStore {

    private final Clock clock = Clock.systemUTC();
    private final Duration ttl = Duration.ofMinutes(10);
    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public void remember(String provider, String state, String frontendOrigin) {
        if (state == null || state.isBlank()) {
            return;
        }
        entries.put(key(provider, state), new Entry(clock.instant().plus(ttl), frontendOrigin));
    }

    @Override
    public OAuthStateContext verifyAndConsume(String provider, String state) {
        if (state == null || state.isBlank()) {
            throw new IllegalStateException("OAuth state가 필요합니다.");
        }
        String k = key(provider, state);
        Entry entry = entries.remove(k);
        if (entry == null || entry.expiresAt().isBefore(clock.instant())) {
            throw new IllegalStateException("OAuth state가 유효하지 않거나 만료되었습니다.");
        }
        return new OAuthStateContext(entry.frontendOrigin());
    }

    private static String key(String provider, String state) {
        return provider.toUpperCase() + ":" + state;
    }

    private record Entry(Instant expiresAt, String frontendOrigin) {}
}
