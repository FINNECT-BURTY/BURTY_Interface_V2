package com.burty.application.service.family;

import com.burty.application.port.out.notify.FamilyAlertPort;
import com.burty.application.port.out.outbox.OutboxEventHandler;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 승인 결과를 요청자(피보호자)에게 알린다. 왜 이체가 안 됐는지 모르면 사용자는 장애로 인식한다. */
@Component
public class TransferApprovalDecisionOutboxHandler implements OutboxEventHandler {

  private final FamilyAlertPort alertPort;

  public TransferApprovalDecisionOutboxHandler(FamilyAlertPort alertPort) {
    this.alertPort = alertPort;
  }

  @Override
  public String eventType() {
    return TransferApprovalOutboxHandler.DECIDED_EVENT;
  }

  @Override
  public void handle(String aggregateId, Map<String, Object> payload) {
    String userId = String.valueOf(payload.get("userId"));
    String decision = String.valueOf(payload.getOrDefault("decision", ""));
    long amount = ((Number) payload.getOrDefault("amount", 0L)).longValue();
    String note = String.valueOf(payload.getOrDefault("note", ""));

    String message =
        switch (decision) {
          case "REJECTED" -> "%,d원 이체가 보호자에 의해 거절되었습니다. %s".formatted(amount, note);
          case "EXPIRED" -> "%,d원 이체가 보호자 승인 기한 만료로 취소되었습니다.".formatted(amount);
          default -> "%,d원 이체 요청이 처리되었습니다. (%s)".formatted(amount, decision);
        };
    alertPort.send(userId, message);
  }
}
