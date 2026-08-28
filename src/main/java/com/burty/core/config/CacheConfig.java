package com.burty.core.config;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * 캐시 설정.
 *
 * <p>이 파일은 전체가 주석 처리되어 있었고 (다른 프로젝트의 package 선언까지 남아 있었다), 그 결과 Redis 를 띄워놓고도 {@code @Cacheable} 이
 * 하나도 없는 상태였다. 기준코드·청년정책처럼 거의 바뀌지 않으면서 자주 조회되는 데이터가 매번 DB 를 때리고 있었다.
 *
 * <p>Redis 가 없는 개발 환경에서는 인메모리 캐시로 자동 강등한다. 캐시 유무로 코드가 달라지지 않게 하기 위함이다.
 *
 * <p><b>제약</b> — {@code @Cacheable} 메서드는 {@code List.of()} 나 {@code Stream.toList()} 가 돌려주는 불변 컬렉션을
 * 반환하면 안 된다. Spring Data Redis 의 타입 해석기가 {@code final} 인 {@code java.*} 타입에는 타입 힌트를 붙이지 않아, 쓸 때는 힌트
 * 없이 나가고 읽을 때는 힌트를 요구해 실패한다. 스프링 데이터 리포지토리는 {@code ArrayList} 를 돌려주므로 현재는 문제가 없다. {@code
 * CacheSerializationTests} 참고.
 */
@Configuration
@EnableCaching
public class CacheConfig {

  /** 캐시 이름 상수. 문자열을 여기저기 흩뿌리면 오타로 캐시가 조용히 갈라진다. */
  public static final String BASE_CODES = "baseCodes";

  public static final String YOUTH_POLICIES = "youthPolicies";
  public static final String POLICY_DETAIL = "policyDetail";
  public static final String CATEGORY_RULES = "categoryRules";

  private static final Map<String, Duration> TTLS =
      Map.of(
          BASE_CODES, Duration.ofHours(6),
          CATEGORY_RULES, Duration.ofMinutes(30),
          YOUTH_POLICIES, Duration.ofMinutes(15),
          POLICY_DETAIL, Duration.ofMinutes(30));

  /**
   * 캐시 값에서 복원을 허용할 타입.
   *
   * <p>캐시 값은 {@code Object} 로 읽으므로 JSON 안의 타입 힌트({@code @class})를 보고 클래스를 만든다. 제한이 없으면 Redis 에 쓰기
   * 권한을 얻은 쪽이 임의 클래스를 지목해 역직렬화 가젯을 노릴 수 있다. 캐시에 실제로 담기는 것은 우리 DTO 와 표준 컬렉션뿐이므로 그 범위로 좁힌다.
   */
  static PolymorphicTypeValidator cacheTypeValidator() {
    return BasicPolymorphicTypeValidator.builder()
        .allowIfSubType("com.burty.")
        .allowIfSubType("java.util.")
        .allowIfSubType("java.time.")
        .allowIfSubType("java.lang.")
        .build();
  }

  /**
   * 캐시 값 직렬화기.
   *
   * <p>{@code redisCacheManager} 는 Redis 가 켜진 환경에서만 만들어져 테스트에서 한 번도 생성되지 않는다. 직렬화 설정이 틀려도 운영에 나가서야
   * 드러나므로, 직렬화기만 따로 만들 수 있게 분리해 {@code CacheSerializationTests} 에서 검증한다.
   */
  static GenericJacksonJsonRedisSerializer cacheValueSerializer() {
    return GenericJacksonJsonRedisSerializer.create(
        builder -> builder.enableDefaultTyping(cacheTypeValidator()));
  }

  @Bean
  @ConditionalOnProperty(name = "burty.redis.enabled", havingValue = "true")
  public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
    RedisCacheConfiguration defaults =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            // null 을 캐시하면 "없음" 이 TTL 동안 굳어버린다.
            .disableCachingNullValues()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(cacheValueSerializer()));

    RedisCacheManager.RedisCacheManagerBuilder builder =
        RedisCacheManager.builder(connectionFactory).cacheDefaults(defaults);
    TTLS.forEach((name, ttl) -> builder.withCacheConfiguration(name, defaults.entryTtl(ttl)));
    return builder.build();
  }

  /** Redis 미사용 환경(로컬 개발·테스트) 폴백. */
  @Bean
  @ConditionalOnMissingBean(CacheManager.class)
  public CacheManager inMemoryCacheManager() {
    return new ConcurrentMapCacheManager(BASE_CODES, YOUTH_POLICIES, POLICY_DETAIL, CATEGORY_RULES);
  }
}
