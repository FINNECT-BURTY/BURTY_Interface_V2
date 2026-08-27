package com.burty.application.service.cashflow;

import com.burty.application.port.out.outbox.OutboxPublisher;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.cashflow.entity.BudgetAlertEntity;
import com.burty.domain.cashflow.entity.BudgetEntity;
import com.burty.domain.cashflow.repository.BudgetAlertRepository;
import com.burty.domain.cashflow.repository.BudgetRepository;
import com.burty.domain.transaction.entity.TransactionEntity;
import com.burty.domain.transaction.repository.TransactionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예산 설정 및 초과 경고.
 *
 * <p>거래 자동 분류와 현금흐름 예측은 이미 있었지만 예산 개념이 없었다. 분류만 해놓고 "그래서 이번 달에 얼마나 쓸 수 있는지" 를 말해주지 못하면 가계 관리 앱으로서
 * 반쪽이다.
 *
 * <p>경고는 두 단계다. 임계치(기본 80%) 도달 시 한 번, 초과 시 한 번. 초과하고 나서야 알리면 이미 늦고, 매 거래마다 알리면 소음이 된다. 중복 발송은
 * {@code tbl_budget_alert} 의 (예산, 기간, 단계) 유니크 제약이 막는다.
 */
@Service
public class BudgetService {

  private static final Logger log = LoggerFactory.getLogger(BudgetService.class);
  private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");

  private final BudgetRepository budgetRepository;
  private final BudgetAlertRepository budgetAlertRepository;
  private final TransactionRepository transactionRepository;
  private final OutboxPublisher outboxPublisher;
  private final Clock clock;

  public BudgetService(
      BudgetRepository budgetRepository,
      BudgetAlertRepository budgetAlertRepository,
      TransactionRepository transactionRepository,
      OutboxPublisher outboxPublisher,
      Clock clock) {
    this.budgetRepository = budgetRepository;
    this.budgetAlertRepository = budgetAlertRepository;
    this.transactionRepository = transactionRepository;
    this.outboxPublisher = outboxPublisher;
    this.clock = clock;
  }

  /** 예산 사용 현황. */
  public record BudgetStatus(
      Long budgetId,
      String categoryCode,
      long budgetAmount,
      long spentAmount,
      long remainingAmount,
      int usagePercent,
      boolean exceeded) {}

  // ── 설정 ──────────────────────────────────────────────────────────────────

  @Transactional
  public BudgetEntity upsert(
      String userId, String categoryCode, long amount, Integer alertThresholdPercent) {
    if (amount <= 0) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "예산은 0보다 커야 합니다.");
    }
    int threshold = alertThresholdPercent == null ? 80 : alertThresholdPercent;
    if (threshold < 1 || threshold > 100) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "경고 임계치는 1~100 사이여야 합니다.");
    }

    String normalizedCategory = normalizeCategory(categoryCode);
    LocalDateTime now = LocalDateTime.now(clock);
    BudgetEntity budget =
        budgetRepository
            .findByUserIdAndCategoryCodeAndPeriodType(
                userId, normalizedCategory, BudgetEntity.PeriodType.MONTHLY)
            .orElseGet(
                () -> {
                  BudgetEntity created = new BudgetEntity();
                  created.setUserId(userId);
                  created.setCategoryCode(normalizedCategory);
                  created.setPeriodType(BudgetEntity.PeriodType.MONTHLY);
                  created.setCreatedAt(now);
                  return created;
                });
    budget.setAmount(amount);
    budget.setAlertThresholdPercent(threshold);
    budget.setActive(true);
    budget.setUpdatedAt(now);
    return budgetRepository.save(budget);
  }

  @Transactional
  public void deactivate(String userId, Long budgetId) {
    BudgetEntity budget =
        budgetRepository
            .findByBudgetIdAndUserId(budgetId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "예산을 찾을 수 없습니다."));
    budget.setActive(false);
    budget.setUpdatedAt(LocalDateTime.now(clock));
    budgetRepository.save(budget);
  }

  // ── 현황 ──────────────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public List<BudgetStatus> currentStatus(String userId) {
    List<BudgetEntity> budgets = budgetRepository.findByUserIdAndActiveTrue(userId);
    if (budgets.isEmpty()) {
      return List.of();
    }
    Map<String, Long> spentByCategory = spentThisMonth(userId);
    long totalSpent = spentByCategory.values().stream().mapToLong(Long::longValue).sum();

    List<BudgetStatus> result = new ArrayList<>();
    for (BudgetEntity budget : budgets) {
      long spent =
          budget.isTotalBudget()
              ? totalSpent
              : spentByCategory.getOrDefault(budget.getCategoryCode(), 0L);
      result.add(toStatus(budget, spent));
    }
    return result;
  }

  // ── 평가 ──────────────────────────────────────────────────────────────────

  /**
   * 예산 초과 여부를 평가하고 필요하면 경고를 발행한다.
   *
   * <p>거래 동기화 직후에 호출한다. 알림 자체는 아웃박스를 통해 나가므로, 평가 트랜잭션이 롤백되면 알림도 나가지 않는다.
   */
  @Transactional
  public int evaluateAndNotify(String userId) {
    List<BudgetEntity> budgets = budgetRepository.findByUserIdAndActiveTrue(userId);
    if (budgets.isEmpty()) {
      return 0;
    }
    String periodKey = LocalDate.now(clock).format(MONTH_KEY);
    Map<String, Long> spentByCategory = spentThisMonth(userId);
    long totalSpent = spentByCategory.values().stream().mapToLong(Long::longValue).sum();

    int published = 0;
    for (BudgetEntity budget : budgets) {
      long spent =
          budget.isTotalBudget()
              ? totalSpent
              : spentByCategory.getOrDefault(budget.getCategoryCode(), 0L);
      BudgetAlertEntity.Level level = levelFor(budget, spent);
      if (level == null) {
        continue;
      }
      if (budgetAlertRepository.existsByBudgetIdAndPeriodKeyAndLevel(
          budget.getBudgetId(), periodKey, level)) {
        continue; // 이번 기간에 이미 알림 — 소음 방지
      }
      recordAndPublish(budget, periodKey, level, spent);
      published++;
    }
    return published;
  }

  private BudgetAlertEntity.Level levelFor(BudgetEntity budget, long spent) {
    long amount = budget.getAmount();
    if (amount <= 0) {
      return null;
    }
    if (spent >= amount) {
      return BudgetAlertEntity.Level.EXCEEDED;
    }
    long thresholdAmount = amount * budget.getAlertThresholdPercent() / 100;
    return spent >= thresholdAmount ? BudgetAlertEntity.Level.THRESHOLD : null;
  }

  private void recordAndPublish(
      BudgetEntity budget, String periodKey, BudgetAlertEntity.Level level, long spent) {
    BudgetAlertEntity alert = new BudgetAlertEntity();
    alert.setBudgetId(budget.getBudgetId());
    alert.setUserId(budget.getUserId());
    alert.setPeriodKey(periodKey);
    alert.setLevel(level);
    alert.setSpentAmount(spent);
    alert.setBudgetAmount(budget.getAmount());
    alert.setNotifiedAt(LocalDateTime.now(clock));
    budgetAlertRepository.save(alert);

    String label = budget.isTotalBudget() ? "전체 지출" : budget.getCategoryCode();
    String message =
        level == BudgetAlertEntity.Level.EXCEEDED
            ? "%s 예산을 초과했습니다. (%,d원 / %,d원)".formatted(label, spent, budget.getAmount())
            : "%s 예산의 %d%% 를 사용했습니다. (%,d원 / %,d원)"
                .formatted(label, spent * 100 / budget.getAmount(), spent, budget.getAmount());

    outboxPublisher.publish(
        "Budget",
        String.valueOf(budget.getBudgetId()),
        BudgetAlertOutboxHandler.EVENT_TYPE,
        Map.of(
            "userId", budget.getUserId(),
            "level", level.name(),
            "message", message,
            "spent", spent,
            "budget", budget.getAmount()));
    log.info(
        "예산 경고 발행 userId={} budgetId={} level={}", budget.getUserId(), budget.getBudgetId(), level);
  }

  // ── 내부 ──────────────────────────────────────────────────────────────────

  /** 이번 달 카테고리별 지출 합계. */
  private Map<String, Long> spentThisMonth(String userId) {
    long numericUserId = parseUserId(userId);
    LocalDate today = LocalDate.now(clock);
    LocalDate from = today.withDayOfMonth(1);

    return transactionRepository
        .findByUserIdAndTxnDateBetweenOrderByTxnDateDesc(numericUserId, from, today)
        .stream()
        .filter(BudgetService::isExpense)
        .collect(
            java.util.stream.Collectors.groupingBy(
                tx ->
                    tx.getExpenseCategoryCode() == null
                        ? "UNCATEGORIZED"
                        : tx.getExpenseCategoryCode(),
                java.util.stream.Collectors.summingLong(tx -> Math.abs(tx.getAmount()))));
  }

  private static boolean isExpense(TransactionEntity tx) {
    return tx.getAmount() != null && tx.getIncomeCategoryCode() == null;
  }

  private BudgetStatus toStatus(BudgetEntity budget, long spent) {
    long amount = budget.getAmount();
    int percent = amount <= 0 ? 0 : (int) Math.min(999, spent * 100 / amount);
    return new BudgetStatus(
        budget.getBudgetId(),
        budget.isTotalBudget() ? null : budget.getCategoryCode(),
        amount,
        spent,
        Math.max(0, amount - spent),
        percent,
        spent >= amount);
  }

  private static String normalizeCategory(String categoryCode) {
    return categoryCode == null || categoryCode.isBlank()
        ? null
        : categoryCode.trim().toUpperCase();
  }

  private static long parseUserId(String userId) {
    try {
      return Long.parseLong(userId);
    } catch (NumberFormatException e) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 사용자 ID입니다.");
    }
  }
}
