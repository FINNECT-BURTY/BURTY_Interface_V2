/**
 *
 *
 * <pre>
 * <b>Description  : 현금흐름 애플리케이션 서비스 (CashflowForecastService)</b>
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

import com.burty.application.port.in.admin.BaseCodeUseCase;
import com.burty.application.port.in.cashflow.CashflowForecastUseCase;
import com.burty.application.port.out.mydata.MyDataPort;
import com.burty.application.service.support.AuditLogger;
import com.burty.core.code.CodeGroups;
import com.burty.core.constant.AppMessages;
import com.burty.core.constant.LogMessages;
import com.burty.domain.admin.entity.BaseCodeEntity;
import com.burty.domain.asset.model.AssetSnapshot;
import com.burty.domain.cashflow.model.CashflowCriteria;
import com.burty.domain.cashflow.model.CashflowEvent;
import com.burty.domain.cashflow.model.CashflowForecast;
import com.burty.domain.cashflow.model.CashflowWhatIfScenario;
import com.burty.domain.cashflow.model.DailyBalancePoint;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CashflowForecastService implements CashflowForecastUseCase {

  private static final Logger log = LoggerFactory.getLogger(CashflowForecastService.class);
  private static final long DEFAULT_LOW_BALANCE_THRESHOLD = 50_000L;
  private static final long DEFAULT_NEGATIVE_THRESHOLD = 0L;

  private final MyDataPort myDataPort;
  private final BaseCodeUseCase baseCodeUseCase;
  private final CashflowUserCriteriaStore criteriaStore;
  private final CashflowEventAssembler eventAssembler;
  private final CashflowForecastSnapshotter snapshotter;
  private final AuditLogger auditLogger;

  @Override
  public CashflowForecast forecast(String userId) {
    AssetSnapshot snapshot = myDataPort.fetchAssetSnapshot(userId);
    LocalDate startDate = LocalDate.now();
    boolean openingBalanceOverridden =
        criteriaStore.settingLong(userId, CashflowUserCriteriaStore.SETTING_OPENING_BALANCE)
            != null;
    long openingBalance =
        criteriaStore.settingLong(
            userId,
            CashflowUserCriteriaStore.SETTING_OPENING_BALANCE,
            estimateOpeningBalance(snapshot));

    var assembled = eventAssembler.buildEvents(userId, startDate, snapshot);
    List<CashflowEvent> events = assembled.events();
    boolean usedDb = assembled.usedDb();

    Long customSafetyBalance =
        criteriaStore.settingLong(userId, CashflowUserCriteriaStore.SETTING_SAFETY_BALANCE);
    long lowThreshold =
        customSafetyBalance == null
            ? lookupLowBalanceThreshold()
            : Math.max(0L, customSafetyBalance);
    boolean customCriteriaUsed =
        usedDb
            || openingBalanceOverridden
            || customSafetyBalance != null
            || criteriaStore.settingLong(
                    userId, CashflowUserCriteriaStore.SETTING_MONTHLY_VARIABLE_BUDGET)
                != null;
    String dataSource = usedDb ? "MYDATA_PLUS_CUSTOM_CRITERIA" : "MYDATA_FALLBACK_ESTIMATE";

    long runningBalance = openingBalance;
    long minimumBalance = openingBalance;
    LocalDate riskDate = null;
    String riskReason = "현재 기준 위험 구간이 없습니다.";
    List<DailyBalancePoint> points = new ArrayList<>();

    for (int i = 0; i < 30; i++) {
      LocalDate day = startDate.plusDays(i);
      long delta = sumOnDay(events, day);
      runningBalance += delta;
      points.add(new DailyBalancePoint(day, runningBalance));

      if (runningBalance < minimumBalance) {
        minimumBalance = runningBalance;
      }
      if (riskDate == null && runningBalance < lowThreshold) {
        riskDate = day;
        riskReason =
            runningBalance < DEFAULT_NEGATIVE_THRESHOLD
                ? "월세/카드/대출 고정지출이 급여 유입보다 먼저 발생해 잔액이 마이너스로 떨어집니다."
                : "예상 잔액이 사용자 안전잔액 기준 " + (lowThreshold / 10_000) + "만원 미만으로 내려갑니다.";
      }
    }

    log.info(LogMessages.Cashflow.FORECAST_KPI, userId, minimumBalance, riskDate, usedDb);

    snapshotter.snapshot(userId, startDate, openingBalance, minimumBalance, riskDate, riskReason);
    return new CashflowForecast(
        userId,
        startDate,
        openingBalance,
        points,
        riskDate,
        riskReason,
        minimumBalance,
        lowThreshold,
        dataSource,
        customCriteriaUsed);
  }

  @Override
  public CashflowWhatIfScenario simulateWhatIf(
      String userId,
      String scenarioName,
      Long extraDailyExpense,
      Long incomeDelta,
      Integer expensePostponeDays) {
    CashflowForecast baseline = forecast(userId);
    long dailyExtra = extraDailyExpense == null ? 0L : Math.max(0L, extraDailyExpense);
    long incomeChange = incomeDelta == null ? 0L : incomeDelta;
    int postpone = expensePostponeDays == null ? 0 : Math.max(0, expensePostponeDays);

    List<DailyBalancePoint> adjustedPoints = new ArrayList<>();
    for (DailyBalancePoint point : baseline.dailyBalances()) {
      long balance = point.balance();
      if (point.date().equals(baseline.generatedDate())) {
        balance += incomeChange;
      }
      balance -= dailyExtra;
      adjustedPoints.add(new DailyBalancePoint(point.date().plusDays(postpone), balance));
    }

    long adjustedMin =
        adjustedPoints.stream()
            .mapToLong(DailyBalancePoint::balance)
            .min()
            .orElse(baseline.minimumBalance());
    String label =
        AppMessages.Cashflow.WHAT_IF_LABEL.formatted(
            scenarioName == null || scenarioName.isBlank() ? "기본" : scenarioName);
    CashflowForecast adjusted =
        new CashflowForecast(
            userId,
            baseline.generatedDate(),
            baseline.openingBalance() + incomeChange,
            adjustedPoints,
            baseline.riskDate(),
            baseline.riskReason(),
            adjustedMin,
            baseline.safetyBalance(),
            baseline.dataSource() + "_WHATIF",
            true);

    return new CashflowWhatIfScenario(
        baseline, adjusted, adjustedMin - baseline.minimumBalance(), label);
  }

  @Override
  public void updateCashflowCriteria(
      String userId, Long safetyBalance, Long openingBalanceOverride, Long monthlyVariableBudget) {
    criteriaStore.updateCriteria(
        userId, safetyBalance, openingBalanceOverride, monthlyVariableBudget);
    auditLogger.logSuccess(
        userId,
        "UPDATE_CASHFLOW_CRITERIA",
        "USER_SETTING",
        "safetyBalance="
            + safetyBalance
            + ", openingBalanceOverride="
            + openingBalanceOverride
            + ", monthlyVariableBudget="
            + monthlyVariableBudget);
  }

  @Override
  public CashflowCriteria getCashflowCriteria(String userId) {
    return criteriaStore.getCriteria(userId);
  }

  private long sumOnDay(List<CashflowEvent> events, LocalDate day) {
    return events.stream()
        .filter(e -> e.eventDate().equals(day))
        .mapToLong(CashflowEvent::amount)
        .sum();
  }

  private long estimateOpeningBalance(AssetSnapshot snapshot) {
    long totalAsset = Math.round(snapshot.totalAsset());
    return Math.max(200_000L, Math.round(totalAsset * 0.015));
  }

  private long lookupLowBalanceThreshold() {
    return baseCodeUseCase
        .lookup(CodeGroups.RISK_LEVEL, "YELLOW")
        .map(BaseCodeEntity::getAttr2)
        .map(this::parseLong)
        .orElse(DEFAULT_LOW_BALANCE_THRESHOLD);
  }

  private long parseLong(String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      return DEFAULT_LOW_BALANCE_THRESHOLD;
    }
  }
}
