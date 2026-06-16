/**
 *
 *
 * <pre>
 * <b>Description  : 현금흐름 (CashflowEventAssembler)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.cashflow
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.application.service.cashflow;

import com.burty.domain.asset.model.AssetSnapshot;
import com.burty.domain.cashflow.entity.CashflowScheduleEntity;
import com.burty.domain.cashflow.entity.RecurringExpenseEntity;
import com.burty.domain.cashflow.model.CashflowEvent;
import com.burty.domain.cashflow.repository.CashflowScheduleRepository;
import com.burty.domain.cashflow.repository.RecurringExpenseRepository;
import com.burty.util.CashflowScheduleDateUtils;
import com.burty.util.PersonaHeuristics;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CashflowEventAssembler {

  private final CashflowScheduleRepository scheduleRepository;
  private final RecurringExpenseRepository recurringExpenseRepository;
  private final CashflowUserCriteriaStore criteriaStore;
  private final CashflowScheduleDateUtils scheduleDateUtils;
  private final PersonaHeuristics personaHeuristics;

  public AssembledEvents buildEvents(String userId, LocalDate startDate, AssetSnapshot snapshot) {
    List<CashflowEvent> dbEvents = buildEventsFromDb(userId, startDate);
    if (dbEvents.isEmpty()) {
      return new AssembledEvents(buildFallbackEvents(startDate, snapshot), false);
    }
    List<CashflowEvent> events = new ArrayList<>(dbEvents);
    addDailyVariableSpend(userId, snapshot, events, startDate);
    return new AssembledEvents(events, true);
  }

  public record AssembledEvents(List<CashflowEvent> events, boolean usedDb) {}

  private List<CashflowEvent> buildEventsFromDb(String userId, LocalDate startDate) {
    Long numericUserId = scheduleDateUtils.parseNumericUserId(userId);
    if (numericUserId == null) return List.of();

    List<CashflowScheduleEntity> schedules =
        scheduleRepository.findByUserIdAndActiveTrue(numericUserId);
    List<RecurringExpenseEntity> recurring =
        recurringExpenseRepository.findByUserIdAndActiveTrue(numericUserId);
    if (schedules.isEmpty() && recurring.isEmpty()) return List.of();

    List<CashflowEvent> events = new ArrayList<>();
    for (CashflowScheduleEntity s : schedules) {
      LocalDate eventDate = scheduleDateUtils.nextOccurrence(startDate, s.getDayOfMonth());
      long signed =
          "INCOME".equalsIgnoreCase(s.getDirection())
              ? Math.abs(s.getAmount())
              : -Math.abs(s.getAmount());
      events.add(
          new CashflowEvent(
              eventDate,
              signed,
              "INCOME".equalsIgnoreCase(s.getDirection()) ? "INCOME" : "EXPENSE",
              s.getScheduleTypeCode(),
              s.getLabel()));
    }
    for (RecurringExpenseEntity r : recurring) {
      LocalDate eventDate = scheduleDateUtils.nextOccurrence(startDate, r.getDayOfMonth());
      events.add(
          new CashflowEvent(
              eventDate,
              -Math.abs(r.getAvgAmount()),
              "EXPENSE",
              r.getExpenseCategoryCode(),
              r.getName()));
    }
    return events;
  }

  private void addDailyVariableSpend(
      String userId, AssetSnapshot snapshot, List<CashflowEvent> events, LocalDate startDate) {
    long variableBudget =
        criteriaStore.settingLong(
            userId,
            CashflowUserCriteriaStore.SETTING_MONTHLY_VARIABLE_BUDGET,
            Math.round(snapshot.monthlySpend() * 0.35));
    if (variableBudget <= 0) return;
    long dailyBudget = Math.max(1_000L, variableBudget / 30);
    for (int i = 0; i < 30; i++) {
      events.add(
          new CashflowEvent(
              startDate.plusDays(i), -dailyBudget, "EXPENSE", "VARIABLE_DAILY", "일상 변동지출 예산"));
    }
  }

  private List<CashflowEvent> buildFallbackEvents(LocalDate startDate, AssetSnapshot snapshot) {
    long monthlySpend = Math.round(snapshot.monthlySpend());
    long monthlyIncome = personaHeuristics.estimateMonthlyIncome(snapshot);

    List<CashflowEvent> events = new ArrayList<>();
    events.add(
        new CashflowEvent(startDate.plusDays(2), monthlyIncome, "INCOME", "SALARY", "월급 유입"));
    events.add(
        new CashflowEvent(
            startDate.plusDays(4), -(long) (monthlySpend * 0.32), "EXPENSE", "RENT", "월세"));
    events.add(
        new CashflowEvent(
            startDate.plusDays(8), -(long) (monthlySpend * 0.25), "EXPENSE", "CARD_BILL", "카드값"));
    events.add(
        new CashflowEvent(
            startDate.plusDays(12), -(long) (monthlySpend * 0.18), "EXPENSE", "LOAN", "대출 상환"));
    events.add(
        new CashflowEvent(
            startDate.plusDays(15), -(long) (monthlySpend * 0.15), "EXPENSE", "LIVING", "생활비"));
    events.add(
        new CashflowEvent(
            startDate.plusDays(21), -(long) (monthlySpend * 0.10), "EXPENSE", "UTILITIES", "공과금"));
    events.add(
        new CashflowEvent(startDate.plusDays(27), monthlyIncome / 2, "INCOME", "SIDE_JOB", "부수입"));
    return events;
  }
}
