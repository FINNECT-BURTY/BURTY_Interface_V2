package com.berty.domain.model;

import java.time.LocalDate;
import java.util.List;

public class CashflowForecast {
    private final String userId;
    private final LocalDate generatedDate;
    private final long openingBalance;
    private final List<DailyBalancePoint> dailyBalances;
    private final LocalDate riskDate;
    private final String riskReason;
    private final long minimumBalance;
    private final long safetyBalance;
    private final String dataSource;
    private final boolean customCriteriaUsed;

    public CashflowForecast(String userId, LocalDate generatedDate, long openingBalance, List<DailyBalancePoint> dailyBalances,
                            LocalDate riskDate, String riskReason, long minimumBalance) {
        this(userId, generatedDate, openingBalance, dailyBalances, riskDate, riskReason, minimumBalance, 50_000L, "UNKNOWN", false);
    }

    public CashflowForecast(String userId, LocalDate generatedDate, long openingBalance, List<DailyBalancePoint> dailyBalances,
                            LocalDate riskDate, String riskReason, long minimumBalance, long safetyBalance,
                            String dataSource, boolean customCriteriaUsed) {
        this.userId = userId;
        this.generatedDate = generatedDate;
        this.openingBalance = openingBalance;
        this.dailyBalances = dailyBalances;
        this.riskDate = riskDate;
        this.riskReason = riskReason;
        this.minimumBalance = minimumBalance;
        this.safetyBalance = safetyBalance;
        this.dataSource = dataSource;
        this.customCriteriaUsed = customCriteriaUsed;
    }

    public String getUserId() { return userId; }
    public LocalDate getGeneratedDate() { return generatedDate; }
    public long getOpeningBalance() { return openingBalance; }
    public List<DailyBalancePoint> getDailyBalances() { return dailyBalances; }
    public LocalDate getRiskDate() { return riskDate; }
    public String getRiskReason() { return riskReason; }
    public long getMinimumBalance() { return minimumBalance; }
    public long getSafetyBalance() { return safetyBalance; }
    public String getDataSource() { return dataSource; }
    public boolean isCustomCriteriaUsed() { return customCriteriaUsed; }
}
