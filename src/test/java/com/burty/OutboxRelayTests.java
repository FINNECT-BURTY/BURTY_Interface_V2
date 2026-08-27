package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.application.port.out.outbox.OutboxEventHandler;
import com.burty.application.port.out.outbox.OutboxPublisher;
import com.burty.application.service.outbox.OutboxRelay;
import com.burty.config.OutboxProperties;
import com.burty.domain.outbox.entity.OutboxEventEntity;
import com.burty.domain.outbox.entity.OutboxEventEntity.Status;
import com.burty.domain.outbox.repository.OutboxEventRepository;
import com.burty.support.IntegrationTestBase;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 아웃박스 릴레이 검증.
 *
 * <p>이 경로는 이체 완료·알림·예산 경고·보호자 승인이 <b>전부</b> 지나간다. 여기서 이벤트가 조용히 사라지면 "이체는 됐는데 보호자에게 알림이 안 감" 같은 사고가
 * 난다. 그래서 정상 경로보다 <b>실패 경로</b>를 집중적으로 확인한다.
 *
 * <ul>
 *   <li>핸들러가 예외를 던지면 재시도로 넘어가는가 (조용히 PUBLISHED 되면 영구 유실)
 *   <li>재시도가 소진되면 DEAD 로 격리되는가 (무한 재시도로 릴레이가 막히면 안 된다)
 *   <li>등록되지 않은 이벤트 타입이 릴레이를 영구히 막지 않는가
 *   <li>백오프가 적용되어 다음 주기에 즉시 재시도하지 않는가
 * </ul>
 */
@SpringBootTest
@org.springframework.context.annotation.Import(OutboxRelayTests.Handlers.class)
class OutboxRelayTests extends IntegrationTestBase {

  private static final String OK_EVENT = "TestOutboxOk";
  private static final String FAIL_EVENT = "TestOutboxAlwaysFails";
  private static final String UNKNOWN_EVENT = "TestOutboxNoHandler";

  @Autowired private OutboxPublisher outboxPublisher;
  @Autowired private OutboxEventRepository outboxEventRepository;
  @Autowired private OutboxRelay outboxRelay;
  @Autowired private OutboxProperties outboxProperties;
  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private RecordingHandler recordingHandler;
  @Autowired private AlwaysFailingHandler failingHandler;

  @BeforeEach
  void reset() {
    recordingHandler.received.clear();
    failingHandler.attempts.set(0);
    outboxEventRepository.deleteAll();
  }

  // ── 정상 경로 ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("발행된 이벤트는 릴레이가 집어 처리기에 전달하고 PUBLISHED 로 확정한다")
  void publishedEventIsDeliveredAndMarkedPublished() {
    publish(OK_EVENT, "agg-1", Map.of("value", "hello"));

    outboxRelay.relayOnce();

    assertEquals(1, recordingHandler.received.size());
    assertEquals("hello", recordingHandler.received.get(0).get("value"));

    OutboxEventEntity event = onlyEvent();
    assertEquals(Status.PUBLISHED, event.getStatus());
    assertNotNull(event.getPublishedAt());
    assertNull(event.getLastError());
  }

  @Test
  @DisplayName("이미 PUBLISHED 된 이벤트는 다시 집지 않는다")
  void publishedEventIsNotPickedUpAgain() {
    publish(OK_EVENT, "agg-2", Map.of("value", "once"));

    outboxRelay.relayOnce();
    outboxRelay.relayOnce();

    assertEquals(1, recordingHandler.received.size(), "같은 이벤트가 두 번 전달되면 안 된다");
  }

  @Test
  @DisplayName("아웃박스 발행은 트랜잭션 안에서만 가능하다 (MANDATORY)")
  void publishRequiresActiveTransaction() {
    // 트랜잭션 없이 호출하면 예외. 이게 없으면 상태 변경과 이벤트의 원자성이 조용히 깨진다.
    assertThrows(
        Exception.class,
        () -> outboxPublisher.publish("Test", "agg-x", OK_EVENT, Map.of("value", "v")));
  }

  // ── 실패 경로 ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("처리기가 실패하면 PUBLISHED 되지 않고 재시도로 넘어간다")
  void failingHandlerSchedulesRetryInsteadOfSilentlySucceeding() {
    publish(FAIL_EVENT, "agg-3", Map.of("value", "boom"));

    outboxRelay.relayOnce();

    OutboxEventEntity event = onlyEvent();
    assertEquals(Status.PENDING, event.getStatus(), "실패했는데 PUBLISHED 면 이벤트가 영구 유실된다");
    assertEquals(1, event.getAttempts());
    assertNotNull(event.getLastError(), "실패 원인이 기록되어야 사후 추적이 가능하다");
    assertEquals(1, failingHandler.attempts.get());
  }

  @Test
  @DisplayName("백오프가 적용되어 다음 주기에 즉시 재시도하지 않는다")
  void retryIsBackedOffNotImmediate() {
    publish(FAIL_EVENT, "agg-4", Map.of("value", "boom"));

    outboxRelay.relayOnce();
    int attemptsAfterFirst = failingHandler.attempts.get();
    outboxRelay.relayOnce(); // 백오프 시간이 지나지 않았으므로 집히지 않아야 한다

    assertEquals(attemptsAfterFirst, failingHandler.attempts.get(), "백오프 없이 즉시 재시도하면 하위 채널을 몰아친다");
    assertTrue(onlyEvent().getNextAttemptAt().isAfter(onlyEvent().getCreatedAt()));
  }

  @Test
  @DisplayName("재시도를 소진하면 DEAD 로 격리되어 릴레이를 막지 않는다")
  void exhaustedRetriesAreQuarantinedAsDead() {
    publish(FAIL_EVENT, "agg-5", Map.of("value", "boom"));

    // 백오프를 무시하고 maxAttempts 만큼 강제로 돌린다.
    for (int i = 0; i < outboxProperties.getMaxAttempts(); i++) {
      makeEventDueNow();
      outboxRelay.relayOnce();
    }

    OutboxEventEntity event = onlyEvent();
    assertEquals(Status.DEAD, event.getStatus());
    assertEquals(outboxProperties.getMaxAttempts(), event.getAttempts());
    assertNotNull(event.getLastError());

    // DEAD 는 더 이상 집히지 않아야 한다 — 안 그러면 릴레이가 이 행에 영원히 매달린다.
    int attemptsBefore = failingHandler.attempts.get();
    makeEventDueNow();
    outboxRelay.relayOnce();
    assertEquals(attemptsBefore, failingHandler.attempts.get());
  }

  @Test
  @DisplayName("처리기가 없는 이벤트는 즉시 DEAD 로 보내 릴레이가 매 주기 같은 행을 집지 않게 한다")
  void unknownEventTypeIsQuarantinedImmediately() {
    publish(UNKNOWN_EVENT, "agg-6", Map.of("value", "orphan"));

    outboxRelay.relayOnce();

    OutboxEventEntity event = onlyEvent();
    assertEquals(Status.DEAD, event.getStatus());
    assertTrue(event.getLastError().contains(UNKNOWN_EVENT));
  }

  // ── 격리 ─────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("한 이벤트의 실패가 같은 배치의 다른 이벤트 처리를 막지 않는다")
  void oneFailureDoesNotBlockOtherEventsInSameBatch() {
    publish(FAIL_EVENT, "agg-7", Map.of("value", "boom"));
    publish(OK_EVENT, "agg-8", Map.of("value", "fine"));

    outboxRelay.relayOnce();

    assertEquals(1, recordingHandler.received.size(), "정상 이벤트는 처리되어야 한다");
    List<OutboxEventEntity> all = outboxEventRepository.findAll();
    assertEquals(1, all.stream().filter(e -> e.getStatus() == Status.PUBLISHED).count());
    assertEquals(1, all.stream().filter(e -> e.getStatus() == Status.PENDING).count());
  }

  // ── 헬퍼 ─────────────────────────────────────────────────────────────────

  /** 아웃박스 발행은 트랜잭션을 요구하므로(MANDATORY) 테스트에서도 트랜잭션으로 감싼다. */
  private void publish(String eventType, String aggregateId, Map<String, ?> payload) {
    transactionTemplate.execute(
        status -> {
          outboxPublisher.publish("Test", aggregateId, eventType, payload);
          return null;
        });
  }

  /** 백오프를 건너뛰기 위해 다음 시도 시각을 과거로 당긴다. */
  private void makeEventDueNow() {
    transactionTemplate.execute(
        status -> {
          outboxEventRepository
              .findAll()
              .forEach(
                  e -> {
                    if (e.getStatus() == Status.PENDING) {
                      e.setNextAttemptAt(e.getCreatedAt().minusMinutes(1));
                      outboxEventRepository.save(e);
                    }
                  });
          return null;
        });
  }

  private OutboxEventEntity onlyEvent() {
    List<OutboxEventEntity> all = outboxEventRepository.findAll();
    assertEquals(1, all.size(), "이벤트가 정확히 한 건이어야 합니다");
    return all.get(0);
  }

  // ── 테스트용 처리기 ────────────────────────────────────────────────────────

  @org.springframework.boot.test.context.TestConfiguration
  static class Handlers {

    @org.springframework.context.annotation.Bean
    RecordingHandler recordingHandler() {
      return new RecordingHandler();
    }

    @org.springframework.context.annotation.Bean
    AlwaysFailingHandler alwaysFailingHandler() {
      return new AlwaysFailingHandler();
    }
  }

  static class RecordingHandler implements OutboxEventHandler {
    final List<Map<String, Object>> received = new ArrayList<>();

    @Override
    public String eventType() {
      return OK_EVENT;
    }

    @Override
    public void handle(String aggregateId, Map<String, Object> payload) {
      received.add(payload);
    }
  }

  /** 항상 실패하는 처리기. 재시도·DEAD 격리 경로를 검증하기 위한 것이다. */
  static class AlwaysFailingHandler implements OutboxEventHandler {
    final AtomicInteger attempts = new AtomicInteger();

    @Override
    public String eventType() {
      return FAIL_EVENT;
    }

    @Override
    public void handle(String aggregateId, Map<String, Object> payload) {
      attempts.incrementAndGet();
      throw new IllegalStateException("의도된 실패 (테스트)");
    }
  }
}
