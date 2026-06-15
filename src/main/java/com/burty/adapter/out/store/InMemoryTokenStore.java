/**
 *
 *
 * <pre>
 * <b>Description  : 외부연동 (InMemoryTokenStore)</b>
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

import com.burty.util.FieldEncryptor;
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
public class InMemoryTokenStore implements TokenStore {
  private final Map<String, String> store = new ConcurrentHashMap<>();
  private final FieldEncryptor fieldEncryptor;

  public InMemoryTokenStore(FieldEncryptor fieldEncryptor) {
    this.fieldEncryptor = fieldEncryptor;
  }

  @Override
  public void put(String key, String value) {
    store.put(key, fieldEncryptor.encrypt(value));
  }

  @Override
  public String get(String key) {
    String stored = store.get(key);
    return stored == null ? null : fieldEncryptor.decrypt(stored);
  }

  @Override
  public void remove(String key) {
    store.remove(key);
  }
}
