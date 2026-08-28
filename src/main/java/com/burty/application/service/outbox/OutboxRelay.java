package com.burty.application.service.outbox;

import com.burty.application.port.out.outbox.OutboxEventHandler;
import com.burty.config.OutboxProperties;
import com.burty.domain.outbox.entity.OutboxEventEntity;
import com.burty.domain.outbox.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 아웃박스 릴레이 — 커밋된 이벤트를 꺼내 실제 부수효과를 수행한다.
 *
 * <p>핵심 규칙 두 가지.
 *
 * <ul>
 *   <li><b>실패는 반드시 남긴다.</b> 처리기가 예외를 던지면 지수 백오프로 재시도하고, 재시도가 소진되면 {@code DEAD} 로 격리한다. 조용히 ACK 하고
 *       버리지 않는다.
 *   <li><b>at-least-once.</b> 발송 후 커밋 전에 죽으면 재발송될 수 있다. 처리기는 멱등해야 한다.
 * </ul>
 */
@Service
public class OutboxRelay {

  private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
  private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {};

  private final OutboxEventRepository repository;
  private final Map<String, OutboxEventHandler> handlers = new HashMap<>();
  private final ObjectMapper objectMapper;
  private final OutboxProperties properties;
  private final Clock clock;
  private final ObjectProvider<MeterRegistry> meterRegistry;

  /**
   * 자기 자신의 프록시.
   *
   * <p>{@link #relay()} 에서 {@code this.relayOnce()} 로 부르면 프록시를 우회해 {@code @Transactional} 이 걸리지
   * 않는다. 생성자로 직접 주입하면 순환 참조가 되므로 지연 조회한다.
   */
  private final ObjectProvider<OutboxRelay> self;

  public OutboxRelay(
      OutboxEventRepository repository,
      List<OutboxEventHandler> handlerList,
      ObjectMapper objectMapper,
      OutboxProperties properties,
      Clock clock,
      ObjectProvider<MeterRegistry> meterRegistry,
      ObjectProvider<OutboxRelay> self) {
    this.repository = repository;
    this.self = self;
    this.objectMapper = objectMapper;
    this.properties = properties;
    this.clock = clock;
    this.meterRegistry = meterRegistry;
    for (OutboxEventHandler handler : handlerList) {
      OutboxEventHandler previous = this.handlers.put(handler.eventType(), handler);
      if (previous != null) {
        throw new IllegalStateException("아웃박스 이벤트 타입 중복 등록: " + handler.eventType());
      }
    }
  }

  /**
   * 스케줄 진입점. 인스턴스가 여러 대여도 한 번만 돌도록 ShedLock 으로 감싼다.
   *
   * <p>락과 스케줄링 관심사만 담당하고 실제 처리는 {@link #relayOnce()} 에 위임한다. 이렇게 나누면 테스트가 락 인프라 없이 릴레이 로직만 직접 검증할 수
   * 있다.
   *
   * <p>반드시 프록시를 거쳐 호출한다. {@code this.relayOnce()} 로 부르면 {@code @Transactional} 이 적용되지 않아 {@code
   * lockPendingBatch} 의 비관적 락 쿼리가 {@code TransactionRequiredException} 으로 매 폴링마다 실패한다.
   */
  @Scheduled(fixedDelayString = "${burty.outbox.poll-interval-ms:1000}")
  @SchedulerLock(name = "outboxRelay", lockAtMostFor = "PT2M", lockAtLeastFor = "PT1S")
  public void relay() {
    self.getObject().relayOnce();
  }

  /**
   * 발행 대기 이벤트를 한 배치 처리한다.
   *
   * @return 처리한 이벤트 수
   */
  @Transactional
  public int relayOnce() {
    if (!properties.isEnabled()) {
      return 0;
    }
    LocalDateTime now = LocalDateTime.now(clock);
    List<OutboxEventEntity> batch =
        repository.lockPendingBatch(now, Limit.of(properties.getBatchSize()));
    for (OutboxEventEntity event : batch) {
      dispatch(event, now);
    }
    return batch.size();
  }

  private void dispatch(OutboxEventEntity event, LocalDateTime now) {
    OutboxEventHandler handler = handlers.get(event.getEventType());
    if (handler == null) {
      // 처리기가 없는 이벤트를 PENDING 으로 두면 릴레이가 매 초 같은 행을 계속 집는다.
      markDead(event, now, "등록된 처리기 없음: " + event.getEventType());
      return;
    }
    try {
      handler.handle(event.getAggregateId(), deserialize(event));
      event.setStatus(OutboxEventEntity.Status.PUBLISHED);
      event.setPublishedAt(now);
      event.setLastError(null);
      count("burty.outbox.published", event.getEventType());
    } catch (Exception e) {
      int attempts = event.getAttempts() + 1;
      event.setAttempts(attempts);
      event.setLastError(truncate(e.getClass().getSimpleName() + ": " + e.getMessage()));
      if (attempts >= properties.getMaxAttempts()) {
        markDead(event, now, event.getLastError());
      } else {
        event.setNextAttemptAt(now.plusSeconds(backoffSeconds(attempts)));
        log.warn(
            "아웃박스 이벤트 발행 실패 — 재시도 예정 eventId={} type={} attempts={}/{} nextAttemptAt={}",
            event.getEventId(),
            event.getEventType(),
            attempts,
            properties.getMaxAttempts(),
            event.getNextAttemptAt(),
            e);
        count("burty.outbox.retried", event.getEventType());
      }
    }
  }

  private void markDead(OutboxEventEntity event, LocalDateTime now, String reason) {
    event.setStatus(OutboxEventEntity.Status.DEAD);
    event.setLastError(truncate(reason));
    event.setNextAttemptAt(now);
    log.error(
        "아웃박스 이벤트 격리(DEAD) — 수동 확인 필요 eventId={} type={} aggregate={}/{} reason={}",
        event.getEventId(),
        event.getEventType(),
        event.getAggregateType(),
        event.getAggregateId(),
        reason);
    count("burty.outbox.dead", event.getEventType());
  }

  private long backoffSeconds(int attempts) {
    long delay = properties.getBackoffBaseSeconds() << Math.min(attempts - 1, 20);
    return Math.min(delay, properties.getMaxBackoffSeconds());
  }

  private Map<String, Object> deserialize(OutboxEventEntity event) {
    try {
      return objectMapper.readValue(event.getPayload(), PAYLOAD_TYPE);
    } catch (Exception e) {
      throw new IllegalStateException("아웃박스 페이로드 역직렬화 실패 eventId=" + event.getEventId(), e);
    }
  }

  private void count(String metric, String eventType) {
    MeterRegistry registry = meterRegistry.getIfAvailable();
    if (registry != null) {
      registry.counter(metric, "eventType", eventType).increment();
    }
  }

  private static String truncate(String value) {
    if (value == null) {
      return null;
    }
    return value.length() <= 500 ? value : value.substring(0, 500);
  }
}
