package com.burty.application.service.cashflow;

import com.burty.application.port.out.outbox.OutboxPublisher;
import com.burty.domain.cashflow.entity.BudgetAlertEntity;
import com.burty.domain.cashflow.entity.BudgetEntity;
import com.burty.domain.cashflow.repository.BudgetAlertRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예산 경고 기록·발행.
 *
 * <p>별도 빈으로 둔 이유가 있다. {@code tbl_budget_alert} 에 (예산, 기간, 단계) 유니크 제약이 있는데, 평가는 "있는지 보고 없으면 넣기" 다.
 * 사용자가 앱에서 동기화를 누르는 순간 배치도 같은 사용자를 동기화하면 둘 다 "없음" 을 보고 둘 다 넣는다.
 *
 * <p>예전에는 이 삽입이 거래 동기화와 같은 트랜잭션이었다. 그래서 <b>경고 중복 하나가 그 동기화로 가져온 거래 전부를 롤백시켰다.</b> 알림이 두 번 나갈 뻔한 것을
 * 막으려다 거래를 잃는 셈이었다.
 *
 * <p>{@code REQUIRES_NEW} 로 분리해 중복이 나면 이 트랜잭션만 되돌린다. 중복은 다른 쪽이 이미 알렸다는 뜻이므로 조용히 넘어가도 된다.
 *
 * <p>대신 바깥 트랜잭션이 롤백돼도 경고는 남는다. 동기화가 실패한 지출로 경고가 나갈 수 있다는 뜻인데, 거래를 통째로 잃는 것보다는 낫다고 봤다.
 */
@Component
public class BudgetAlertPublisher {

  private static final Logger log = LoggerFactory.getLogger(BudgetAlertPublisher.class);

  private final BudgetAlertRepository budgetAlertRepository;
  private final OutboxPublisher outboxPublisher;
  private final Clock clock;

  public BudgetAlertPublisher(
      BudgetAlertRepository budgetAlertRepository, OutboxPublisher outboxPublisher, Clock clock) {
    this.budgetAlertRepository = budgetAlertRepository;
    this.outboxPublisher = outboxPublisher;
    this.clock = clock;
  }

  /**
   * 경고를 기록하고 알림을 예약한다.
   *
   * @return {@code true} 면 이 호출이 발행했다, {@code false} 면 이미 다른 쪽이 발행했다
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean publish(
      BudgetEntity budget, String periodKey, BudgetAlertEntity.Level level, long spent) {
    try {
      BudgetAlertEntity alert = new BudgetAlertEntity();
      alert.setBudgetId(budget.getBudgetId());
      alert.setUserId(budget.getUserId());
      alert.setPeriodKey(periodKey);
      alert.setLevel(level);
      alert.setSpentAmount(spent);
      alert.setBudgetAmount(budget.getAmount());
      alert.setNotifiedAt(LocalDateTime.now(clock));
      budgetAlertRepository.saveAndFlush(alert);

      outboxPublisher.publish(
          "Budget",
          String.valueOf(budget.getBudgetId()),
          BudgetAlertOutboxHandler.EVENT_TYPE,
          Map.of(
              "userId", budget.getUserId(),
              "level", level.name(),
              "message", message(budget, level, spent),
              "spent", spent,
              "budget", budget.getAmount()));
      log.info(
          "예산 경고 발행 userId={} budgetId={} level={}",
          budget.getUserId(),
          budget.getBudgetId(),
          level);
      return true;
    } catch (DataIntegrityViolationException e) {
      // 같은 기간·단계의 경고가 이미 있다. 다른 쪽이 알렸다는 뜻이므로 넘어간다.
      log.debug(
          "예산 경고 중복 — 이미 발행됨 budgetId={} period={} level={}",
          budget.getBudgetId(),
          periodKey,
          level);
      return false;
    }
  }

  private String message(BudgetEntity budget, BudgetAlertEntity.Level level, long spent) {
    String label = budget.isTotalBudget() ? "전체 지출" : budget.getCategoryCode();
    if (level == BudgetAlertEntity.Level.EXCEEDED) {
      return "%s 예산을 초과했습니다. (%,d원 / %,d원)".formatted(label, spent, budget.getAmount());
    }
    return "%s 예산의 %d%% 를 사용했습니다. (%,d원 / %,d원)"
        .formatted(label, spent * 100 / budget.getAmount(), spent, budget.getAmount());
  }
}
