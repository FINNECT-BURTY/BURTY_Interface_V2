package com.berty.adapter.in.web.dto;

public class FamilyDashboardResponse {
    private String userId;
    private int alertCount;
    private int unusualTransactionCount;
    private int monthlyReportDeliveredCount;

    public FamilyDashboardResponse() {}
    public FamilyDashboardResponse(String userId, int alertCount, int unusualTransactionCount, int monthlyReportDeliveredCount) {
        this.userId = userId;
        this.alertCount = alertCount;
        this.unusualTransactionCount = unusualTransactionCount;
        this.monthlyReportDeliveredCount = monthlyReportDeliveredCount;
    }
    public String getUserId() { return userId; }
    public int getAlertCount() { return alertCount; }
    public int getUnusualTransactionCount() { return unusualTransactionCount; }
    public int getMonthlyReportDeliveredCount() { return monthlyReportDeliveredCount; }
}
