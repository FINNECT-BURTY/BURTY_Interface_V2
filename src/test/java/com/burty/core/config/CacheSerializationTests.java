package com.burty.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * Redis 캐시 값 직렬화 검증.
 *
 * <p>{@code redisCacheManager} 는 {@code burty.redis.enabled=true} 인 환경에서만 만들어진다. 즉 테스트 컨텍스트에서는 한 번도
 * 생성되지 않는다. 직렬화 설정이 틀려도 운영에 나가서야 드러나므로, 직렬화기만 떼어내 여기서 검증한다.
 */
class CacheSerializationTests {

  private final GenericJacksonJsonRedisSerializer serializer = CacheConfig.cacheValueSerializer();

  record CachedPolicy(String code, String name, LocalDate validUntil) {}

  @Test
  @DisplayName("DTO 를 왕복해도 구체 타입과 값이 유지된다")
  void roundTripsConcreteType() {
    CachedPolicy original = new CachedPolicy("YP-1", "청년월세", LocalDate.of(2026, 12, 31));

    Object restored = serializer.deserialize(serializer.serialize(original));

    // 캐시는 Object 로 읽는다. 타입 힌트가 없으면 Map 으로 돌아와 @Cacheable 이 ClassCastException 을 낸다.
    assertInstanceOf(CachedPolicy.class, restored);
    assertEquals(original, restored);
  }

  @Test
  @DisplayName("리스트를 왕복해도 원소 타입이 유지된다")
  void roundTripsListOfDtos() {
    List<CachedPolicy> original =
        new ArrayList<>(
            List.of(
                new CachedPolicy("YP-1", "청년월세", LocalDate.of(2026, 12, 31)),
                new CachedPolicy("YP-2", "청년내일채움", LocalDate.of(2027, 1, 31))));

    Object restored = serializer.deserialize(serializer.serialize(original));

    assertInstanceOf(List.class, restored);
    assertEquals(original, restored);
  }

  @Test
  @DisplayName("불변 JDK 컬렉션은 캐시에 담을 수 없다 (알려진 제약)")
  void immutableJdkCollectionsCannotBeCached() {
    // Spring Data Redis 의 타입 해석기는 final 인 java.* 타입에 타입 힌트를 붙이지 않는다.
    // List.of() 와 Stream.toList() 가 돌려주는 ImmutableCollections$ListN 이 여기 걸려,
    // 쓸 때는 타입 힌트 없이 배열로 나가고 읽을 때는 힌트를 요구해 실패한다.
    //
    // @Cacheable 메서드는 ArrayList 를 돌려줘야 한다. 스프링 데이터 리포지토리는 ArrayList 를
    // 돌려주므로 지금은 문제가 없지만, 중간에 .stream()...toList() 를 끼우면 이 경로를 밟는다.
    // 운영(burty.redis.enabled=true)에서만 드러나므로 여기에 못 박아 둔다.
    byte[] bytes = serializer.serialize(List.of(new CachedPolicy("YP-1", "청년월세", null)));

    assertThrows(SerializationException.class, () -> serializer.deserialize(bytes));
  }

  @Test
  @DisplayName("허용 목록 밖의 타입은 복원하지 않는다")
  void rejectsTypesOutsideAllowList() {
    // Redis 에 쓰기 권한을 얻은 쪽이 타입 힌트를 바꿔 임의 클래스를 만들게 하는 것을 막는다.
    byte[] forged = ("{\"@class\":\"javax.naming.ldap.Rdn\",\"value\":\"x\"}").getBytes();

    assertThrows(SerializationException.class, () -> serializer.deserialize(forged));
  }
}
