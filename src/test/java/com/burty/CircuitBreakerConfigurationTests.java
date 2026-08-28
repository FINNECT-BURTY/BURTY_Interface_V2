package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.support.IntegrationTestBase;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 서킷브레이커 구성 검증.
 *
 * <p>{@code resilience4j-spring-boot3} 스타터를 걷어내고 레지스트리를 직접 만들었다. 스타터가 해주던 일(프로퍼티 바인딩, 메트릭 등록)을 우리가
 * 대신 하므로, <b>정말로 같은 결과가 나오는지</b> 확인해야 한다.
 *
 * <p>특히 메트릭은 운영 알람({@code CircuitBreakerOpen})이 쓰는 지표다. 빠지면 외부 연동이 차단돼도 아무도 모른다.
 */
@SpringBootTest
class CircuitBreakerConfigurationTests extends IntegrationTestBase {

  @Autowired private CircuitBreakerRegistry registry;
  @Autowired private MeterRegistry meterRegistry;

  @Test
  @DisplayName("설정 파일의 인스턴스가 등록되고 값이 반영된다")
  void configuredInstancesAreRegisteredWithTheirSettings() {
    CircuitBreaker openbanking = registry.circuitBreaker("openbanking");
    assertNotNull(openbanking);

    var config = openbanking.getCircuitBreakerConfig();
    assertEquals(10, config.getSlidingWindowSize());
    assertEquals(50.0f, config.getFailureRateThreshold());
    assertEquals(
        30_000L,
        config.getWaitIntervalFunctionInOpenState().apply(1),
        "차단 유지 시간이 설정값(30s)과 달라지면 복구 시점이 바뀐다");
  }

  @Test
  @DisplayName("마이데이터 인스턴스도 개별 설정을 가진다")
  void mydataInstanceIsConfiguredSeparately() {
    var config = registry.circuitBreaker("mydata").getCircuitBreakerConfig();
    assertEquals(10, config.getSlidingWindowSize());
    assertEquals(50.0f, config.getFailureRateThreshold());
  }

  @Test
  @DisplayName("첫 호출 전에도 인스턴스가 존재해 메트릭이 노출된다")
  void instancesArePreCreatedSoMetricsExistBeforeFirstCall() {
    // 미리 만들어 두지 않으면 "아직 안 불림" 과 "차단됨" 을 대시보드에서 구분할 수 없다.
    assertTrue(
        registry.getAllCircuitBreakers().stream()
            .anyMatch(cb -> cb.getName().equals("openbanking")));
    assertTrue(
        registry.getAllCircuitBreakers().stream().anyMatch(cb -> cb.getName().equals("mydata")));
  }

  @Test
  @DisplayName("알람이 참조하는 메트릭이 실제로 등록된다")
  void circuitBreakerStateMetricIsRegistered() {
    // infra/observability/alert-rules.yml 의 CircuitBreakerOpen 이 이 지표를 본다.
    // 스타터를 걷어내면서 메트릭 등록이 빠지기 쉬운 지점이라 명시적으로 고정한다.
    boolean found =
        meterRegistry.getMeters().stream()
            .anyMatch(m -> m.getId().getName().equals("resilience4j.circuitbreaker.state"));
    assertTrue(found, "resilience4j.circuitbreaker.state 가 없으면 외부 연동 차단을 감지하지 못한다");
  }

  @Test
  @DisplayName("실패가 임계치를 넘으면 차단되고 이후 호출이 거부된다")
  void breakerOpensAfterFailureThresholdIsExceeded() {
    // 이 테스트 전용 인스턴스. 공유 인스턴스를 오염시키면 다른 테스트가 흔들린다.
    CircuitBreaker breaker = registry.circuitBreaker("test-open-behaviour");

    // minimumNumberOfCalls(기본 5) 이상 실패시켜야 실패율 계산이 시작된다.
    for (int i = 0; i < 10; i++) {
      try {
        breaker.executeSupplier(
            () -> {
              throw new IllegalStateException("의도된 실패");
            });
      } catch (RuntimeException ignored) {
        // 실패를 쌓는 것이 목적이다.
      }
    }

    assertEquals(CircuitBreaker.State.OPEN, breaker.getState());
    assertThrows(
        CallNotPermittedException.class, () -> breaker.executeSupplier(() -> "이건 실행되면 안 된다"));
  }

  @Test
  @DisplayName("등록되지 않은 이름도 기본 설정으로 동작한다")
  void unknownInstanceFallsBackToDefaults() {
    CircuitBreaker adhoc = registry.circuitBreaker("not-in-properties");
    assertNotNull(adhoc);
    assertEquals("성공", adhoc.executeSupplier(() -> "성공"));
  }
}
