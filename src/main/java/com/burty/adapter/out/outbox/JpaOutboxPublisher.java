package com.burty.adapter.out.outbox;

import com.burty.application.port.out.outbox.OutboxPublisher;
import com.burty.domain.outbox.entity.OutboxEventEntity;
import com.burty.domain.outbox.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaOutboxPublisher implements OutboxPublisher {

  private final OutboxEventRepository repository;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public JpaOutboxPublisher(
      OutboxEventRepository repository, ObjectMapper objectMapper, Clock clock) {
    this.repository = repository;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /** {@code MANDATORY} — 호출자의 트랜잭션에 반드시 참여한다. 트랜잭션 없이 호출하면 예외가 나므로, 아웃박스의 원자성 보장이 조용히 깨지는 일이 없다. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void publish(
      String aggregateType, String aggregateId, String eventType, Map<String, ?> payload) {
    OutboxEventEntity event = new OutboxEventEntity();
    event.setAggregateType(aggregateType);
    event.setAggregateId(aggregateId);
    event.setEventType(eventType);
    event.setPayload(serialize(payload));
    event.setStatus(OutboxEventEntity.Status.PENDING);
    event.setAttempts(0);
    LocalDateTime now = LocalDateTime.now(clock);
    event.setCreatedAt(now);
    event.setNextAttemptAt(now);
    repository.save(event);
  }

  private String serialize(Map<String, ?> payload) {
    try {
      return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
    } catch (JsonProcessingException e) {
      // 페이로드는 우리가 만든 Map 이므로 여기 오면 프로그래밍 오류다. 조용히 넘기면 이벤트가 유실된다.
      throw new IllegalStateException("아웃박스 페이로드 직렬화 실패: " + eventDescription(payload), e);
    }
  }

  private static String eventDescription(Map<String, ?> payload) {
    return payload == null ? "null" : String.join(",", payload.keySet());
  }
}
