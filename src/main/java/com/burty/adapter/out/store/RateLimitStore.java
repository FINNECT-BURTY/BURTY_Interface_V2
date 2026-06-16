package com.burty.adapter.out.store;

public interface RateLimitStore {

  /** 윈도우 내 요청 허용 여부. */
  boolean tryConsume(String key, int maxRequests, long windowMillis);
}
