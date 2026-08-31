/**
 *
 *
 * <pre>
 * <b>Description  : 외부연동 (InMemoryChallengeStore)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.out.store
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.adapter.out.store;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "burty.redis",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
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
  public void remove(String key) {
    store.remove(key);
  }

  @Override
  public boolean consume(String key) {
    return store.remove(key) != null;
  }

  private static class Entry {
    private final String value;
    private final LocalDateTime expireAt;

    private Entry(String value, LocalDateTime expireAt) {
      this.value = value;
      this.expireAt = expireAt;
    }
  }
}
