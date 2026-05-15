package com.burty.adapter.in.web.dto;

import java.util.List;

public class ActionFeedbackSummaryResponse {
    private String userId;
    private int totalExecutedActions;
    private int acceptedCount;
    private int rejectedCount;
    private List<String> recentExecutedActions;

    public ActionFeedbackSummaryResponse() {}

    public ActionFeedbackSummaryResponse(String userId, int totalExecutedActions, int acceptedCount, int rejectedCount, List<String> recentExecutedActions) {
        this.userId = userId;
        this.totalExecutedActions = totalExecutedActions;
        this.acceptedCount = acceptedCount;
        this.rejectedCount = rejectedCount;
        this.recentExecutedActions = recentExecutedActions;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getTotalExecutedActions() { return totalExecutedActions; }
    public void setTotalExecutedActions(int totalExecutedActions) { this.totalExecutedActions = totalExecutedActions; }
    public int getAcceptedCount() { return acceptedCount; }
    public void setAcceptedCount(int acceptedCount) { this.acceptedCount = acceptedCount; }
    public int getRejectedCount() { return rejectedCount; }
    public void setRejectedCount(int rejectedCount) { this.rejectedCount = rejectedCount; }
    public List<String> getRecentExecutedActions() { return recentExecutedActions; }
    public void setRecentExecutedActions(List<String> recentExecutedActions) { this.recentExecutedActions = recentExecutedActions; }
}
