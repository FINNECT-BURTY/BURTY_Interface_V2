package com.burty.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 캐시 설정.
 *
 * <p>이 파일은 전체가 주석 처리되어 있었고 (다른 프로젝트의 package 선언까지 남아 있었다), 그 결과 Redis 를 띄워놓고도 {@code @Cacheable} 이
 * 하나도 없는 상태였다. 기준코드·청년정책처럼 거의 바뀌지 않으면서 자주 조회되는 데이터가 매번 DB 를 때리고 있었다.
 *
 * <p>Redis 가 없는 개발 환경에서는 인메모리 캐시로 자동 강등한다. 캐시 유무로 코드가 달라지지 않게 하기 위함이다.
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

  @Bean
  @ConditionalOnProperty(name = "burty.redis.enabled", havingValue = "true")
  public CacheManager redisCacheManager(
      RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
    RedisCacheConfiguration defaults =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            // null 을 캐시하면 "없음" 이 TTL 동안 굳어버린다.
            .disableCachingNullValues()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer(objectMapper)));

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
