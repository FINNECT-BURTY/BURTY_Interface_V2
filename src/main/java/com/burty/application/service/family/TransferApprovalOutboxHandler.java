package com.burty.application.service.family;

import com.burty.application.port.out.notify.FamilyAlertPort;
import com.burty.application.port.out.outbox.OutboxEventHandler;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 보호자 승인 요청 알림. 보호자가 즉시 반응해야 하므로 이체 완료 알림보다 우선순위가 높다. */
@Component
public class TransferApprovalOutboxHandler implements OutboxEventHandler {

  public static final String REQUESTED_EVENT = "TransferApprovalRequested";
  public static final String DECIDED_EVENT = "TransferApprovalDecided";

  private final FamilyAlertPort alertPort;

  public TransferApprovalOutboxHandler(FamilyAlertPort alertPort) {
    this.alertPort = alertPort;
  }

  @Override
  public String eventType() {
    return REQUESTED_EVENT;
  }

  @Override
  public void handle(String aggregateId, Map<String, Object> payload) {
    String guardianUserId = String.valueOf(payload.get("guardianUserId"));
    long amount = ((Number) payload.getOrDefault("amount", 0L)).longValue();
    String toAccount = String.valueOf(payload.getOrDefault("toAccountMasked", ""));
    String expiresAt = String.valueOf(payload.getOrDefault("expiresAt", ""));

    alertPort.send(
        guardianUserId,
        "[승인 요청] 보호 중인 계정에서 %,d원을 %s 계좌로 이체하려 합니다. %s 까지 승인하지 않으면 자동 취소됩니다."
            .formatted(amount, toAccount, expiresAt));
  }
}
