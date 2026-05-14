package com.berty.adapter.in.web.dto;

public class ActionTrackingResponse {
    private String userId;
    private String actionType;
    private long executedCount;
    private long acceptedCount;
    private long rejectedCount;
    private long currentProjectedBalance;
    private String currentRiskLevel;

    public ActionTrackingResponse() {}

    public ActionTrackingResponse(String userId, String actionType, long executedCount, long acceptedCount,
                                  long rejectedCount, long currentProjectedBalance, String currentRiskLevel) {
        this.userId = userId;
        this.actionType = actionType;
        this.executedCount = executedCount;
        this.acceptedCount = acceptedCount;
        this.rejectedCount = rejectedCount;
        this.currentProjectedBalance = currentProjectedBalance;
        this.currentRiskLevel = currentRiskLevel;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public long getExecutedCount() { return executedCount; }
    public void setExecutedCount(long executedCount) { this.executedCount = executedCount; }
    public long getAcceptedCount() { return acceptedCount; }
    public void setAcceptedCount(long acceptedCount) { this.acceptedCount = acceptedCount; }
    public long getRejectedCount() { return rejectedCount; }
    public void setRejectedCount(long rejectedCount) { this.rejectedCount = rejectedCount; }
    public long getCurrentProjectedBalance() { return currentProjectedBalance; }
    public void setCurrentProjectedBalance(long currentProjectedBalance) { this.currentProjectedBalance = currentProjectedBalance; }
    public String getCurrentRiskLevel() { return currentRiskLevel; }
    public void setCurrentRiskLevel(String currentRiskLevel) { this.currentRiskLevel = currentRiskLevel; }
}
