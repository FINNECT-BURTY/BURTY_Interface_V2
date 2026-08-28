package com.burty.core.config;

import com.burty.config.CircuitBreakerProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 서킷브레이커 레지스트리를 직접 구성한다.
 *
 * <p>{@code resilience4j-spring-boot3} 스타터를 쓰지 않는 이유는 {@link CircuitBreakerProperties} 에 적었다. 요약하면
 * 그 스타터가 Spring Boot 4 를 거부해서 버전을 올릴 수 없기 때문이고, 실제로 필요한 건 레지스트리 하나뿐이라 직접 만드는 편이 낫다고 판단했다.
 *
 * <p>메트릭 등록도 직접 한다. {@code resilience4j_circuitbreaker_state} 는 운영 알람이 쓰는 지표라 ({@code
 * CircuitBreakerOpen}) 빠지면 외부 연동 차단을 감지하지 못한다.
 */
@Configuration
public class CircuitBreakerConfiguration {

  private static final Logger log = LoggerFactory.getLogger(CircuitBreakerConfiguration.class);

  @Bean
  public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerProperties properties) {
    CircuitBreakerConfig defaults = toConfig(properties.getDefaults());

    Map<String, CircuitBreakerConfig> configs =
        properties.getInstances().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> toConfig(entry.getValue())));

    // 이름별 설정을 미리 등록해 둔다. 등록되지 않은 이름으로 요청이 오면 defaults 가 쓰인다.
    CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaults);
    configs.forEach(registry::addConfiguration);

    // 미리 인스턴스를 만들어 둔다. 그래야 첫 호출 전에도 메트릭이 노출되어,
    // "아직 한 번도 안 불린 것" 과 "차단된 것" 을 대시보드에서 구분할 수 있다.
    properties.getInstances().keySet().forEach(name -> registry.circuitBreaker(name, name));

    log.info("서킷브레이커 구성 완료 — 인스턴스 {}", properties.getInstances().keySet());
    return registry;
  }

  /**
   * 메트릭 바인딩.
   *
   * <p>{@code MeterRegistry} 가 없는 환경(일부 테스트)에서도 뜨도록 {@code ObjectProvider} 로 받는다.
   */
  @Bean
  public TaggedCircuitBreakerMetrics circuitBreakerMetrics(
      CircuitBreakerRegistry registry, ObjectProvider<MeterRegistry> meterRegistry) {
    TaggedCircuitBreakerMetrics metrics =
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry);
    MeterRegistry meters = meterRegistry.getIfAvailable();
    if (meters != null) {
      metrics.bindTo(meters);
    } else {
      log.debug("MeterRegistry 없음 — 서킷브레이커 메트릭 미등록");
    }
    return metrics;
  }

  private static CircuitBreakerConfig toConfig(CircuitBreakerProperties.InstanceConfig c) {
    return CircuitBreakerConfig.custom()
        .slidingWindowSize(c.getSlidingWindowSize())
        .failureRateThreshold(c.getFailureRateThreshold())
        .waitDurationInOpenState(c.getWaitDurationInOpenState())
        .permittedNumberOfCallsInHalfOpenState(c.getPermittedNumberOfCallsInHalfOpenState())
        .minimumNumberOfCalls(c.getMinimumNumberOfCalls())
        .build();
  }
}
