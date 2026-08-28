package com.burty;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.burty.application.port.out.audit.AuditLogPort;
import com.burty.application.service.support.AuditLogger;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 감사 기록 저장 실패 처리.
 *
 * <p>감사 기록은 규제 대응 자료다. 저장에 실패해도 업무 흐름은 계속 가야 하지만, 조용히 넘어가서도 안 된다. 예전에는 {@code log.warn} 한 줄이 전부라
 * 기록이 사라진 것을 아무도 몰랐고, 나중에 남는 것은 "그 시점 기록이 없다" 는 사실뿐이었다.
 *
 * <p>아웃박스·이체 정합성·암호키 교체에는 전부 메트릭과 알림이 있었는데 감사 로그에만 없었다.
 */
class AuditLoggerFailureTests {

  private static final String METRIC = "burty_audit_save_failed_total";

  private final MeterRegistry registry = new SimpleMeterRegistry();

  @Test
  @DisplayName("저장에 실패해도 업무 흐름을 끊지 않는다")
  void doesNotPropagateFailure() {
    AuditLogger logger =
        loggerWith(
            event -> {
              throw new IllegalStateException("DB down");
            });

    assertDoesNotThrow(() -> logger.logSuccess("1", "TRANSFER_EXECUTED", "ORD-1", null));
  }

  @Test
  @DisplayName("저장에 실패하면 알림용 카운터를 올린다")
  void countsFailureForAlerting() {
    AuditLogger logger =
        loggerWith(
            event -> {
              throw new IllegalStateException("DB down");
            });

    logger.logSuccess("1", "TRANSFER_EXECUTED", "ORD-1", null);
    logger.logFailure("1", "TRANSFER_EXECUTED", "ORD-2", "rejected");

    // 알림 규칙 AuditLogSaveFailed 가 이 이름에 걸려 있다.
    var counter = registry.find(METRIC).tag("action", "TRANSFER_EXECUTED").counter();
    assertNotNull(counter, METRIC + " 카운터가 없다 — 기록 유실이 조용히 넘어간다");
    assertEquals(2.0, counter.count());
  }

  @Test
  @DisplayName("정상 저장 시에는 카운터를 올리지 않는다")
  void doesNotCountOnSuccess() {
    AuditLogger logger = loggerWith(event -> {});

    logger.logSuccess("1", "TRANSFER_EXECUTED", "ORD-1", null);

    assertEquals(0, registry.find(METRIC).counters().size());
  }

  private AuditLogger loggerWith(AuditLogPort port) {
    return new AuditLogger(port, new SingletonProvider<>(registry));
  }

  /** {@code ObjectProvider} 중 이 클래스가 쓰는 메서드만 구현한 최소 구현. */
  private record SingletonProvider<T>(T instance) implements ObjectProvider<T> {

    @Override
    public T getObject() {
      return instance;
    }

    @Override
    public T getObject(Object... args) {
      return instance;
    }

    @Override
    public T getIfAvailable() {
      return instance;
    }

    @Override
    public T getIfUnique() {
      return instance;
    }
  }
}
