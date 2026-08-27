package com.burty.application.service.transaction;

import com.burty.application.port.out.notify.FamilyAlertPort;
import com.burty.application.port.out.outbox.OutboxEventHandler;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 소액 거래 묶음 요약 알림. 건별로 다 보내면 알림이 소음이 되어 정작 중요한 알림이 묻힌다. */
@Component
public class TransactionSummaryOutboxHandler implements OutboxEventHandler {

  private final FamilyAlertPort alertPort;

  public TransactionSummaryOutboxHandler(FamilyAlertPort alertPort) {
    this.alertPort = alertPort;
  }

  @Override
  public String eventType() {
    return TransactionNotificationOutboxHandler.SUMMARY_EVENT_TYPE;
  }

  @Override
  public void handle(String aggregateId, Map<String, Object> payload) {
    String userId = String.valueOf(payload.get("userId"));
    int count = ((Number) payload.getOrDefault("count", 0)).intValue();
    if (count <= 0) {
      return;
    }
    alertPort.send(userId, "새로운 거래 %d건이 확인되었습니다.".formatted(count));
  }
}
