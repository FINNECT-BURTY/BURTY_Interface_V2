package com.burty.application.port.out.outbox;

import java.util.Map;

/**
 * 도메인 이벤트를 아웃박스에 적재한다.
 *
 * <p>반드시 <b>비즈니스 트랜잭션 안에서</b> 호출해야 한다. 그래야 상태 변경과 이벤트가 원자적으로 함께 커밋된다.
 */
public interface OutboxPublisher {

  void publish(String aggregateType, String aggregateId, String eventType, Map<String, ?> payload);
}
