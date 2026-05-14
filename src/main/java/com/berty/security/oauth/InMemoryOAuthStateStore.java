package com.berty.security.oauth;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryOAuthStateStore implements OAuthStateStore {

    private final Clock clock = Clock.systemUTC();
    private final Duration ttl = Duration.ofMinutes(10);
    private final ConcurrentHashMap<String, Instant> entries = new ConcurrentHashMap<>();

    @Override
    public void remember(String provider, String state) {
        if (state == null || state.isBlank()) {
            return;
        }
        entries.put(key(provider, state), clock.instant().plus(ttl));
    }

    @Override
    public void verifyAndConsume(String provider, String state) {
        if (state == null || state.isBlank()) {
            throw new IllegalStateException("OAuth state가 필요합니다.");
        }
        String k = key(provider, state);
        Instant exp = entries.remove(k);
        if (exp == null || exp.isBefore(clock.instant())) {
            throw new IllegalStateException("OAuth state가 유효하지 않거나 만료되었습니다.");
        }
    }

    private static String key(String provider, String state) {
        return provider.toUpperCase() + ":" + state;
    }
}
