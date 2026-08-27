package com.burty.application.service.finance.outbox;

import com.burty.application.port.out.notify.FamilyAlertPort;
import com.burty.application.port.out.outbox.OutboxEventHandler;
import com.burty.config.TransferPolicyProperties;
import com.burty.core.constant.AppMessages;
import com.burty.domain.finance.repository.RegisteredAccountRepository;
import java.time.Clock;
import java.time.LocalTime;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 이체 완료 이벤트 → 보호자 알림.
 *
 * <p>예전에는 이 로직이 이체 트랜잭션 한가운데에 있었다. 알림 발송이 느리면 DB 커넥션을 잡고 있었고, 알림이 실패하면 이체 전체가 롤백될 수 있었으며, 반대로 이체가
 * 롤백되어도 알림은 이미 나간 뒤였다. 아웃박스로 분리하면 이체는 이체대로 확정되고 알림은 재시도 가능한 별도 작업이 된다.
 */
@Component
public class TransferExecutedOutboxHandler implements OutboxEventHandler {

  public static final String EVENT_TYPE = "TransferExecuted";

  private final FamilyAlertPort familyAlertPort;
  private final RegisteredAccountRepository registeredAccountRepository;
  private final TransferPolicyProperties policy;
  private final Clock clock;

  public TransferExecutedOutboxHandler(
      FamilyAlertPort familyAlertPort,
      RegisteredAccountRepository registeredAccountRepository,
      TransferPolicyProperties policy,
      Clock clock) {
    this.familyAlertPort = familyAlertPort;
    this.registeredAccountRepository = registeredAccountRepository;
    this.policy = policy;
    this.clock = clock;
  }

  @Override
  public String eventType() {
    return EVENT_TYPE;
  }

  @Override
  public void handle(String aggregateId, Map<String, Object> payload) {
    String userId = String.valueOf(payload.get("userId"));
    long amount = ((Number) payload.getOrDefault("amount", 0L)).longValue();
    String toAccountMasked = String.valueOf(payload.getOrDefault("toAccountMasked", ""));

    if (isSuspicious(userId, toAccountMasked, amount)) {
      familyAlertPort.send(userId, AppMessages.Transfer.FAMILY_ALERT_SUSPICIOUS);
    }
    if (amount >= policy.getFamilyAlertThreshold()
        || isSuspicious(userId, toAccountMasked, amount)) {
      familyAlertPort.send(
          userId, AppMessages.Transfer.FAMILY_ALERT_TRANSFER.formatted(amount, toAccountMasked));
    }
  }

  private boolean isSuspicious(String userId, String toAccountMasked, long amount) {
    // 시간 판정은 주입된 Clock 기준이다. 예전에는 LocalTime.now() 를 직접 불러 테스트가 불가능했다.
    LocalTime now = LocalTime.now(clock);
    boolean night = now.isAfter(policy.getNightStart()) || now.isBefore(policy.getNightEnd());
    boolean unregistered =
        !registeredAccountRepository.existsByUserIdAndAccountNo(userId, toAccountMasked);
    boolean large = amount >= policy.getLargeTransferThreshold();
    return night || unregistered || large;
  }
}
