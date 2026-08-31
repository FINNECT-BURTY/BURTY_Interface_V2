package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.application.port.out.outbox.OutboxEventHandler;
import com.burty.application.port.out.outbox.OutboxPublisher;
import com.burty.application.service.admin.OperationsService;
import com.burty.application.service.outbox.OutboxRelay;
import com.burty.config.OutboxProperties;
import com.burty.core.exception.BusinessException;
import com.burty.domain.outbox.entity.OutboxEventEntity;
import com.burty.domain.outbox.entity.OutboxEventEntity.Status;
import com.burty.domain.outbox.repository.OutboxEventRepository;
import com.burty.support.IntegrationTestBase;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * DLQ 재투입 리허설.
 *
 * <p>운영 런북에 "원인을 고친 뒤 재처리한다" 는 절차가 적혀 있지만 <b>한 번도 실행된 적이 없었다.</b> 백업·복구 리허설을 실제로 돌렸을 때 스크립트 결함이 세 개
 * 나왔던 것을 생각하면, 문서에만 있는 절차는 검증된 절차가 아니다.
 *
 * <p>여기서 절차 전체를 한 번 돌린다.
 *
 * <ol>
 *   <li>처리기가 계속 실패해 이벤트가 {@code DEAD} 로 격리되는가
 *   <li>운영자가 DLQ 목록에서 그 이벤트를 볼 수 있는가
 *   <li>원인을 고친 뒤 재투입하면 실제로 다시 처리되는가
 *   <li>고치지 않고 재투입하면 다시 격리되는가 (런북의 경고가 사실인가)
 * </ol>
 */
@SpringBootTest
@Import(DeadLetterRedriveRehearsalTests.Handlers.class)
class DeadLetterRedriveRehearsalTests extends IntegrationTestBase {

  private static final String EVENT = "RedriveRehearsalEvent";
  private static final String OPERATOR = "operator-1";

  @Autowired private OutboxPublisher outboxPublisher;
  @Autowired private OutboxEventRepository outboxEventRepository;
  @Autowired private OutboxRelay outboxRelay;
  @Autowired private OutboxProperties outboxProperties;
  @Autowired private OperationsService operationsService;
  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private ToggleableHandler handler;

  @BeforeEach
  void reset() {
    handler.failing.set(true);
    handler.received.clear();
    outboxEventRepository.deleteAll();
  }

  @Test
  @DisplayName("런북 절차대로 격리 → 확인 → 수정 → 재투입 이 동작한다")
  void redriveProcedureWorks() {
    publish();
    driveToDeadLetter();

    // 2) 운영자가 DLQ 에서 볼 수 있어야 한다. 보이지 않으면 재투입할 대상을 고를 수 없다.
    List<OutboxEventEntity> dead = operationsService.deadLetters(100);
    assertEquals(1, dead.size(), "격리된 이벤트가 DLQ 목록에 보이지 않는다");
    Long eventId = dead.get(0).getEventId();

    // 3) 원인을 고친다 → 재투입 → 실제로 처리돼야 한다.
    handler.failing.set(false);
    assertEquals(1, operationsService.redriveDeadLetters(OPERATOR, List.of(eventId)));
    assertEquals(Status.PENDING, outboxEventRepository.findById(eventId).orElseThrow().getStatus());

    outboxRelay.relayOnce();

    assertEquals(1, handler.received.size(), "재투입한 이벤트가 처리되지 않았다");
    assertEquals(
        Status.PUBLISHED, outboxEventRepository.findById(eventId).orElseThrow().getStatus());
  }

  @Test
  @DisplayName("원인을 고치지 않고 재투입하면 다시 격리된다")
  void redriveWithoutFixingReturnsToDeadLetter() {
    publish();
    driveToDeadLetter();
    Long eventId = operationsService.deadLetters(100).get(0).getEventId();

    // 런북의 경고("고치지 않고 재처리하면 다시 DLQ로 갑니다")가 사실인지 확인한다.
    operationsService.redriveDeadLetters(OPERATOR, List.of(eventId));
    driveToDeadLetter();

    assertEquals(Status.DEAD, outboxEventRepository.findById(eventId).orElseThrow().getStatus());
  }

  @Test
  @DisplayName("DEAD 가 아닌 이벤트는 재투입 대상이 아니다")
  void onlyDeadEventsAreRedriven() {
    publish();
    Long eventId = outboxEventRepository.findAll().get(0).getEventId();

    // PENDING 인 이벤트를 되돌리면 시도 횟수가 초기화돼 백오프가 무의미해진다.
    assertEquals(0, operationsService.redriveDeadLetters(OPERATOR, List.of(eventId)));
  }

  @Test
  @DisplayName("대상 없이 호출하면 거절한다")
  void emptyRedriveIsRejected() {
    assertThrows(
        BusinessException.class, () -> operationsService.redriveDeadLetters(OPERATOR, List.of()));
  }

  // ── 도우미 ────────────────────────────────────────────────────────────────

  private void publish() {
    transactionTemplate.execute(
        status -> {
          outboxPublisher.publish("Rehearsal", "agg-1", EVENT, Map.of("value", "x"));
          return null;
        });
  }

  /** 재시도가 소진돼 DEAD 로 갈 때까지 돌린다. 백오프는 다음 시도 시각을 앞당겨 건너뛴다. */
  private void driveToDeadLetter() {
    for (int i = 0; i <= outboxProperties.getMaxAttempts() + 1; i++) {
      makeDueNow();
      outboxRelay.relayOnce();
      if (outboxEventRepository.findAll().stream().allMatch(e -> e.getStatus() == Status.DEAD)) {
        return;
      }
    }
    assertTrue(false, "재시도를 소진했는데 DEAD 로 격리되지 않았다");
  }

  private void makeDueNow() {
    transactionTemplate.execute(
        status -> {
          outboxEventRepository
              .findAll()
              .forEach(e -> e.setNextAttemptAt(java.time.LocalDateTime.now().minusMinutes(1)));
          return null;
        });
  }

  static class Handlers {
    @Bean
    ToggleableHandler toggleableHandler() {
      return new ToggleableHandler();
    }
  }

  /** 실패/성공을 바꿀 수 있는 처리기. "원인을 고쳤다" 를 흉내낸다. */
  static class ToggleableHandler implements OutboxEventHandler {
    final AtomicBoolean failing = new AtomicBoolean(true);
    final List<Map<String, Object>> received = new ArrayList<>();

    @Override
    public String eventType() {
      return EVENT;
    }

    @Override
    public void handle(String aggregateId, Map<String, Object> payload) {
      if (failing.get()) {
        throw new IllegalStateException("의도된 실패");
      }
      received.add(payload);
    }
  }
}
