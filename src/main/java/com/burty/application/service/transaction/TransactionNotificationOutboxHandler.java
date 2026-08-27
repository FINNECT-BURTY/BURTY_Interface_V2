package com.burty.application.service.transaction;

import com.burty.application.port.out.notify.FamilyAlertPort;
import com.burty.application.port.out.outbox.OutboxEventHandler;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 신규 거래 실시간 알림.
 *
 * <p>예전에는 거래 동기화가 하루 1회 배치(04시)뿐이라 "방금 결제됐다" 를 알릴 방법이 없었다. 시니어 보호가 핵심 가치인 서비스에서 이상 결제를 하루 뒤에 아는 것은
 * 사실상 못 막는 것과 같다.
 *
 * <p>동기화 주기 자체를 짧게 하는 것과 별개로, 신규 거래가 확인되는 즉시 알림이 나가도록 아웃박스로 연결한다.
 */
@Component
public class TransactionNotificationOutboxHandler implements OutboxEventHandler {

  public static final String EVENT_TYPE = "TransactionDetected";
  public static final String SUMMARY_EVENT_TYPE = "TransactionSummary";

  private final FamilyAlertPort alertPort;

  public TransactionNotificationOutboxHandler(FamilyAlertPort alertPort) {
    this.alertPort = alertPort;
  }

  @Override
  public String eventType() {
    return EVENT_TYPE;
  }

  @Override
  public void handle(String aggregateId, Map<String, Object> payload) {
    String userId = String.valueOf(payload.get("userId"));
    long amount = ((Number) payload.getOrDefault("amount", 0L)).longValue();
    String merchant = String.valueOf(payload.getOrDefault("merchant", ""));
    alertPort.send(
        userId,
        "%,d원이 결제되었습니다.%s".formatted(amount, merchant.isBlank() ? "" : " (" + merchant + ")"));
  }
}
