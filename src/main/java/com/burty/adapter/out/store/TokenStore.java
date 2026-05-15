package com.burty.adapter.out.store;

public interface TokenStore {
    void put(String key, String value);
    String get(String key);
}
