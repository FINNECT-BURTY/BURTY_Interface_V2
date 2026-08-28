package com.burty.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 서킷브레이커 설정.
 *
 * <p>예전에는 {@code resilience4j-spring-boot3} 스타터가 이 설정을 자동으로 바인딩했다. 그런데 그 스타터는 2.4.0 부터 {@code
 * SpringBoot3Verifier} 로 Spring Boot 4 를 명시적으로 거부한다. 즉 스타터를 쓰는 한 resilience4j 를 올릴 수 없고, Boot 4 대응
 * 모듈은 아직 배포되지 않았다.
 *
 * <p>실제로 필요한 것은 {@code CircuitBreakerRegistry} 하나뿐이다. 스타터는 그것을 만들어 주는 역할이었고, 설정 바인딩은 우리가 직접 해도 된다.
 * 그래서 스타터를 걷어내고 core 모듈만 쓴다. 프로퍼티 키는 그대로 유지해 기존 설정 파일을 바꾸지 않는다.
 */
@Component
@ConfigurationProperties(prefix = "resilience4j.circuitbreaker")
@Getter
@Setter
public class CircuitBreakerProperties {

  /** 인스턴스 이름 → 설정. 이름은 {@code ResilientHttpExecutor} 가 넘기는 값과 같아야 한다. */
  private Map<String, InstanceConfig> instances = new LinkedHashMap<>();

  /** 등록되지 않은 이름으로 요청이 오면 이 값이 쓰인다. */
  private InstanceConfig defaults = new InstanceConfig();

  @Getter
  @Setter
  public static class InstanceConfig {

    /** 실패율을 계산할 최근 호출 수. */
    private int slidingWindowSize = 10;

    /** 이 비율(%)을 넘으면 차단한다. */
    private float failureRateThreshold = 50;

    /** 차단 상태를 유지하는 시간. 지나면 half-open 으로 넘어가 탐색 호출을 보낸다. */
    private Duration waitDurationInOpenState = Duration.ofSeconds(30);

    /** half-open 상태에서 허용할 탐색 호출 수. */
    private int permittedNumberOfCallsInHalfOpenState = 3;

    /** 이 횟수 미만이면 실패율을 계산하지 않는다. 표본이 적을 때 성급히 차단하는 것을 막는다. */
    private int minimumNumberOfCalls = 5;
  }
}
