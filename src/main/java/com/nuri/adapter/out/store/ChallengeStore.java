package com.nuri.adapter.out.store;

public interface ChallengeStore {
    void put(String key, String value, long ttlSeconds);
    String get(String key);
    void remove(String key);
}
