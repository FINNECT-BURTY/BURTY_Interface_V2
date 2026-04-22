package com.nuri.adapter.out.store;

public interface TokenStore {
    void put(String key, String value);
    String get(String key);
}
