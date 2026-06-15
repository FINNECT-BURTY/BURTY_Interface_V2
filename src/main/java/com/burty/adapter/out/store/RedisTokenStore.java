/**
 *
 *
 * <pre>
 * <b>Description  : 외부연동 (RedisTokenStore)</b>
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "burty.redis", name = "enabled", havingValue = "true")
public class RedisTokenStore implements TokenStore {
  private final StringRedisTemplate redisTemplate;
  private final FieldEncryptor fieldEncryptor;

  public RedisTokenStore(StringRedisTemplate redisTemplate, FieldEncryptor fieldEncryptor) {
    this.redisTemplate = redisTemplate;
    this.fieldEncryptor = fieldEncryptor;
  }

  @Override
  public void put(String key, String value) {
    redisTemplate.opsForValue().set("burty:token:" + key, fieldEncryptor.encrypt(value));
  }

  @Override
  public String get(String key) {
    String stored = redisTemplate.opsForValue().get("burty:token:" + key);
    return stored == null ? null : fieldEncryptor.decrypt(stored);
  }

  @Override
  public void remove(String key) {
    redisTemplate.delete("burty:token:" + key);
  }
}
