package com.burty.domain.model;

import java.time.LocalDate;

public class RiskAssessment {
    private final String userId;
    private final String level;
    private final long threshold;
    private final String reason;
    private final LocalDate riskDate;
    private final long projectedBalance;

    public RiskAssessment(String userId, String level, long threshold, String reason, LocalDate riskDate, long projectedBalance) {
        this.userId = userId;
        this.level = level;
        this.threshold = threshold;
        this.reason = reason;
        this.riskDate = riskDate;
        this.projectedBalance = projectedBalance;
    }

    public String getUserId() { return userId; }
    public String getLevel() { return level; }
    public long getThreshold() { return threshold; }
    public String getReason() { return reason; }
    public LocalDate getRiskDate() { return riskDate; }
    public long getProjectedBalance() { return projectedBalance; }
}
