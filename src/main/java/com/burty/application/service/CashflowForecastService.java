package com.burty.application.service;

import com.burty.application.port.in.BaseCodeUseCase;
import com.burty.application.port.in.CashflowForecastUseCase;
import com.burty.application.port.out.MyDataPort;
import com.burty.core.code.CodeGroups;
import com.burty.domain.entity.BaseCodeEntity;
import com.burty.domain.entity.CashflowForecastEntity;
import com.burty.domain.entity.CashflowScheduleEntity;
import com.burty.domain.entity.RecurringExpenseEntity;
import com.burty.domain.model.AssetSnapshot;
import com.burty.domain.model.CashflowEvent;
import com.burty.domain.model.CashflowForecast;
import com.burty.domain.model.DailyBalancePoint;
import com.burty.domain.repository.CashflowForecastHistoryRepository;
import com.burty.domain.repository.CashflowScheduleRepository;
import com.burty.domain.repository.RecurringExpenseRepository;
import com.burty.domain.repository.UserSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class CashflowForecastService implements CashflowForecastUseCase {
    private static final Logger log = LoggerFactory.getLogger(CashflowForecastService.class);
    private static final long DEFAULT_LOW_BALANCE_THRESHOLD = 50_000L;
    private static final long DEFAULT_NEGATIVE_THRESHOLD = 0L;
    private static final String SETTING_SAFETY_BALANCE = "SAFETY_BALANCE";
    private static final String SETTING_OPENING_BALANCE = "OPENING_BALANCE_OVERRIDE";
    private static final String SETTING_MONTHLY_VARIABLE_BUDGET = "MONTHLY_VARIABLE_BUDGET";

    private final MyDataPort myDataPort;
    private final CashflowScheduleRepository scheduleRepository;
    private final RecurringExpenseRepository recurringExpenseRepository;
    private final BaseCodeUseCase baseCodeUseCase;
    private final CashflowForecastHistoryRepository historyRepository;
    private final UserSettingRepository userSettingRepository;

    public CashflowForecastService(MyDataPort myDataPort, CashflowScheduleRepository scheduleRepository,
                                   RecurringExpenseRepository recurringExpenseRepository, BaseCodeUseCase baseCodeUseCase,
                                   CashflowForecastHistoryRepository historyRepository, UserSettingRepository userSettingRepository) {
        this.myDataPort = myDataPort;
        this.scheduleRepository = scheduleRepository;
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.baseCodeUseCase = baseCodeUseCase;
        this.historyRepository = historyRepository;
        this.userSettingRepository = userSettingRepository;
    }

    @Override
    public CashflowForecast forecast(String userId) {
        AssetSnapshot snapshot = myDataPort.fetchAssetSnapshot(userId);
        LocalDate startDate = LocalDate.now();
        boolean openingBalanceOverridden = settingLong(userId, SETTING_OPENING_BALANCE) != null;
        long openingBalance = settingLong(userId, SETTING_OPENING_BALANCE, estimateOpeningBalance(snapshot));

        List<CashflowEvent> events = buildEventsFromDb(userId, startDate);
        boolean usedDb = !events.isEmpty();
        if (events.isEmpty()) {
            events = buildFallbackEvents(startDate, snapshot);
        } else {
            events = new ArrayList<>(events);
            addDailyVariableSpend(userId, snapshot, events, startDate);
        }

        Long customSafetyBalance = settingLong(userId, SETTING_SAFETY_BALANCE);
        long lowThreshold = customSafetyBalance == null ? lookupLowBalanceThreshold() : Math.max(0L, customSafetyBalance);
        boolean customCriteriaUsed = usedDb || openingBalanceOverridden || customSafetyBalance != null
                || settingLong(userId, SETTING_MONTHLY_VARIABLE_BUDGET) != null;
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
                riskReason = runningBalance < DEFAULT_NEGATIVE_THRESHOLD
                        ? "월세/카드/대출 고정지출이 급여 유입보다 먼저 발생해 잔액이 마이너스로 떨어집니다."
                        : "예상 잔액이 사용자 안전잔액 기준 " + (lowThreshold / 10_000) + "만원 미만으로 내려갑니다.";
            }
        }

        log.info("KPI forecast userId={} minBalance={} riskDate={} usedDb={}",
                userId, minimumBalance, riskDate, usedDb);

        snapshotForecast(userId, startDate, openingBalance, minimumBalance, riskDate, riskReason);
        return new CashflowForecast(userId, startDate, openingBalance, points, riskDate, riskReason,
                minimumBalance, lowThreshold, dataSource, customCriteriaUsed);
    }

    private void snapshotForecast(String userId, java.time.LocalDate forecastDate, long openingBalance, long minimumBalance, LocalDate riskDate, String riskReason) {
        try {
            CashflowForecastEntity entity = historyRepository
                    .findByUserIdAndForecastDate(userId, forecastDate)
                    .orElseGet(CashflowForecastEntity::new);
            entity.setUserId(userId);
            entity.setForecastDate(forecastDate);
            entity.setOpeningBalance(openingBalance);
            entity.setMinimumBalance(minimumBalance);
            entity.setRiskDate(riskDate);
            entity.setRiskReason(riskReason);
            entity.setHorizonDays(30);
            historyRepository.save(entity);
        } catch (Exception e) {
            log.warn("Forecast snapshot save failed userId={} err={}", userId, e.getMessage());
        }
    }

    private long sumOnDay(List<CashflowEvent> events, LocalDate day) {
        return events.stream()
                .filter(e -> e.getEventDate().equals(day))
                .mapToLong(CashflowEvent::getAmount)
                .sum();
    }

    private List<CashflowEvent> buildEventsFromDb(String userId, LocalDate startDate) {
        Long numericUserId = parseUserId(userId);
        if (numericUserId == null) return List.of();

        List<CashflowScheduleEntity> schedules = scheduleRepository.findByUserIdAndActiveTrue(numericUserId);
        List<RecurringExpenseEntity> recurring = recurringExpenseRepository.findByUserIdAndActiveTrue(numericUserId);
        if (schedules.isEmpty() && recurring.isEmpty()) return List.of();

        List<CashflowEvent> events = new ArrayList<>();
        for (CashflowScheduleEntity s : schedules) {
            LocalDate eventDate = nextOccurrence(startDate, s.getDayOfMonth());
            long signed = "INCOME".equalsIgnoreCase(s.getDirection()) ? Math.abs(s.getAmount()) : -Math.abs(s.getAmount());
            events.add(new CashflowEvent(eventDate, signed, "INCOME".equalsIgnoreCase(s.getDirection()) ? "INCOME" : "EXPENSE",
                    s.getScheduleTypeCode(), s.getLabel()));
        }
        for (RecurringExpenseEntity r : recurring) {
            LocalDate eventDate = nextOccurrence(startDate, r.getDayOfMonth());
            events.add(new CashflowEvent(eventDate, -Math.abs(r.getAvgAmount()), "EXPENSE",
                    r.getExpenseCategoryCode(), r.getName()));
        }
        return events;
    }

    private void addDailyVariableSpend(String userId, AssetSnapshot snapshot, List<CashflowEvent> events, LocalDate startDate) {
        long variableBudget = settingLong(userId, SETTING_MONTHLY_VARIABLE_BUDGET,
                Math.round(snapshot.getMonthlySpend() * 0.35));
        if (variableBudget <= 0) return;
        long dailyBudget = Math.max(1_000L, variableBudget / 30);
        for (int i = 0; i < 30; i++) {
            events.add(new CashflowEvent(startDate.plusDays(i), -dailyBudget, "EXPENSE", "VARIABLE_DAILY", "일상 변동지출 예산"));
        }
    }

    private List<CashflowEvent> buildFallbackEvents(LocalDate startDate, AssetSnapshot snapshot) {
        long monthlySpend = Math.round(snapshot.getMonthlySpend());
        long monthlyIncome = estimateMonthlyIncome(snapshot);

        List<CashflowEvent> events = new ArrayList<>();
        events.add(new CashflowEvent(startDate.plusDays(2), monthlyIncome, "INCOME", "SALARY", "월급 유입"));
        events.add(new CashflowEvent(startDate.plusDays(4), -(long) (monthlySpend * 0.32), "EXPENSE", "RENT", "월세"));
        events.add(new CashflowEvent(startDate.plusDays(8), -(long) (monthlySpend * 0.25), "EXPENSE", "CARD_BILL", "카드값"));
        events.add(new CashflowEvent(startDate.plusDays(12), -(long) (monthlySpend * 0.18), "EXPENSE", "LOAN", "대출 상환"));
        events.add(new CashflowEvent(startDate.plusDays(15), -(long) (monthlySpend * 0.15), "EXPENSE", "LIVING", "생활비"));
        events.add(new CashflowEvent(startDate.plusDays(21), -(long) (monthlySpend * 0.10), "EXPENSE", "UTILITIES", "공과금"));
        events.add(new CashflowEvent(startDate.plusDays(27), monthlyIncome / 2, "INCOME", "SIDE_JOB", "부수입"));
        return events;
    }

    private LocalDate nextOccurrence(LocalDate startDate, int dayOfMonth) {
        int day = Math.max(1, Math.min(28, dayOfMonth));
        LocalDate candidate = startDate.withDayOfMonth(Math.min(day, startDate.lengthOfMonth()));
        if (candidate.isBefore(startDate)) {
            candidate = candidate.plusMonths(1);
            candidate = candidate.withDayOfMonth(Math.min(day, candidate.lengthOfMonth()));
        }
        return candidate;
    }

    private long estimateOpeningBalance(AssetSnapshot snapshot) {
        long totalAsset = Math.round(snapshot.getTotalAsset());
        return Math.max(200_000L, Math.round(totalAsset * 0.015));
    }

    private long estimateMonthlyIncome(AssetSnapshot snapshot) {
        long spend = Math.round(snapshot.getMonthlySpend());
        return Math.max(1_000_000L, Math.round(spend * 1.1));
    }

    private long lookupLowBalanceThreshold() {
        return baseCodeUseCase.lookup(CodeGroups.RISK_LEVEL, "YELLOW")
                .map(BaseCodeEntity::getAttr2)
                .map(this::parseLong)
                .orElse(DEFAULT_LOW_BALANCE_THRESHOLD);
    }

    private long settingLong(String userId, String key, long defaultValue) {
        Long value = settingLong(userId, key);
        return value == null ? defaultValue : value;
    }

    private Long settingLong(String userId, String key) {
        return userSettingRepository.findByUserIdAndSettingKey(userId, key)
                .map(it -> it.getSettingValueLong() != null ? it.getSettingValueLong() : parseNullableLong(it.getSettingValueStr()))
                .orElse(null);
    }

    private Long parseNullableLong(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return DEFAULT_LOW_BALANCE_THRESHOLD;
        }
    }

    private Long parseUserId(String userId) {
        if (userId == null || userId.isBlank()) return null;
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
