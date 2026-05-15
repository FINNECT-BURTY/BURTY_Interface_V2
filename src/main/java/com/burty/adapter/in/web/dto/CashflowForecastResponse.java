package com.burty.adapter.in.web.dto;

import com.burty.domain.model.DailyBalancePoint;

import java.time.LocalDate;
import java.util.List;

public class CashflowForecastResponse {
    private String userId;
    private LocalDate generatedDate;
    private long openingBalance;
    private long minimumBalance;
    private LocalDate riskDate;
    private String riskReason;
    private List<DailyBalancePoint> dailyBalances;
    private long safetyBalance;
    private String dataSource;
    private boolean customCriteriaUsed;

    public CashflowForecastResponse() {}

    public CashflowForecastResponse(String userId, LocalDate generatedDate, long openingBalance, long minimumBalance, LocalDate riskDate,
                                    String riskReason, List<DailyBalancePoint> dailyBalances) {
        this(userId, generatedDate, openingBalance, minimumBalance, riskDate, riskReason, dailyBalances, 50_000L, "UNKNOWN", false);
    }

    public CashflowForecastResponse(String userId, LocalDate generatedDate, long openingBalance, long minimumBalance, LocalDate riskDate,
                                    String riskReason, List<DailyBalancePoint> dailyBalances, long safetyBalance,
                                    String dataSource, boolean customCriteriaUsed) {
        this.userId = userId;
        this.generatedDate = generatedDate;
        this.openingBalance = openingBalance;
        this.minimumBalance = minimumBalance;
        this.riskDate = riskDate;
        this.riskReason = riskReason;
        this.dailyBalances = dailyBalances;
        this.safetyBalance = safetyBalance;
        this.dataSource = dataSource;
        this.customCriteriaUsed = customCriteriaUsed;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDate getGeneratedDate() { return generatedDate; }
    public void setGeneratedDate(LocalDate generatedDate) { this.generatedDate = generatedDate; }
    public long getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(long openingBalance) { this.openingBalance = openingBalance; }
    public long getMinimumBalance() { return minimumBalance; }
    public void setMinimumBalance(long minimumBalance) { this.minimumBalance = minimumBalance; }
    public LocalDate getRiskDate() { return riskDate; }
    public void setRiskDate(LocalDate riskDate) { this.riskDate = riskDate; }
    public String getRiskReason() { return riskReason; }
    public void setRiskReason(String riskReason) { this.riskReason = riskReason; }
    public List<DailyBalancePoint> getDailyBalances() { return dailyBalances; }
    public void setDailyBalances(List<DailyBalancePoint> dailyBalances) { this.dailyBalances = dailyBalances; }
    public long getSafetyBalance() { return safetyBalance; }
    public void setSafetyBalance(long safetyBalance) { this.safetyBalance = safetyBalance; }
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    public boolean isCustomCriteriaUsed() { return customCriteriaUsed; }
    public void setCustomCriteriaUsed(boolean customCriteriaUsed) { this.customCriteriaUsed = customCriteriaUsed; }
}
