package com.burty.adapter.in.web.dto;

import java.time.LocalDate;

public class RiskAssessmentResponse {
    private String userId;
    private String level;
    private long threshold;
    private String reason;
    private LocalDate riskDate;
    private long projectedBalance;

    public RiskAssessmentResponse() {}

    public RiskAssessmentResponse(String userId, String level, long threshold, String reason, LocalDate riskDate, long projectedBalance) {
        this.userId = userId;
        this.level = level;
        this.threshold = threshold;
        this.reason = reason;
        this.riskDate = riskDate;
        this.projectedBalance = projectedBalance;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public long getThreshold() { return threshold; }
    public void setThreshold(long threshold) { this.threshold = threshold; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDate getRiskDate() { return riskDate; }
    public void setRiskDate(LocalDate riskDate) { this.riskDate = riskDate; }
    public long getProjectedBalance() { return projectedBalance; }
    public void setProjectedBalance(long projectedBalance) { this.projectedBalance = projectedBalance; }
}
