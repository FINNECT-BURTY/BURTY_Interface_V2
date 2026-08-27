package com.burty.application.service.cashflow;

import com.burty.application.port.out.notify.FamilyAlertPort;
import com.burty.application.port.out.outbox.OutboxEventHandler;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 예산 경고 → 사용자 알림.
 *
 * <p>평가와 발송을 아웃박스로 분리한 이유는 이체 알림과 같다. 평가 트랜잭션이 롤백되면 알림도 나가지 않아야 하고, 알림 채널 장애가 예산 평가를 실패시켜서는 안 된다.
 */
@Component
public class BudgetAlertOutboxHandler implements OutboxEventHandler {

  public static final String EVENT_TYPE = "BudgetAlert";

  private final FamilyAlertPort alertPort;

  public BudgetAlertOutboxHandler(FamilyAlertPort alertPort) {
    this.alertPort = alertPort;
  }

  @Override
  public String eventType() {
    return EVENT_TYPE;
  }

  @Override
  public void handle(String aggregateId, Map<String, Object> payload) {
    String userId = String.valueOf(payload.get("userId"));
    String message = String.valueOf(payload.get("message"));
    alertPort.send(userId, message);
  }
}
