package com.burty.application.port.out.outbox;

import java.util.Map;

/** 아웃박스 이벤트 타입별 실제 부수효과 처리기. 릴레이가 {@link #eventType()} 로 매칭해 호출한다. */
public interface OutboxEventHandler {

  String eventType();

  /**
   * 부수효과를 수행한다.
   *
   * <p>실패하면 예외를 던져야 한다. 릴레이가 재시도 스케줄을 잡는다. 예외를 삼키면 이벤트가 발행 성공으로 표시되어 영구 유실된다.
   */
  void handle(String aggregateId, Map<String, Object> payload);
}
