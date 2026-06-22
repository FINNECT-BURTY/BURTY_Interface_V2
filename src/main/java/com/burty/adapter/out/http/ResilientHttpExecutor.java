package com.burty.adapter.out.http;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.stereotype.Component;

/** Resilience4j circuit breaker 로 외부 HTTP 호출을 보호합니다. */
@Component
public class ResilientHttpExecutor {

  private final CircuitBreakerRegistry circuitBreakerRegistry;

  public ResilientHttpExecutor(CircuitBreakerRegistry circuitBreakerRegistry) {
    this.circuitBreakerRegistry = circuitBreakerRegistry;
  }

  public <T> T execute(String instanceName, java.util.function.Supplier<T> supplier) {
    CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(instanceName);
    try {
      return circuitBreaker.executeSupplier(supplier);
    } catch (CallNotPermittedException e) {
      throw new com.burty.core.exception.BusinessException(
          com.burty.core.error.enums.ErrorCode.EXTERNAL_API_ERROR, "외부 API 일시 중단: " + instanceName);
    }
  }
}
