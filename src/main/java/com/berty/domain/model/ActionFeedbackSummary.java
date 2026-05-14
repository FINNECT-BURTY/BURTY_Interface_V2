package com.berty.domain.model;

import java.util.List;

public class ActionFeedbackSummary {
    private final String userId;
    private final int totalExecutedActions;
    private final int acceptedCount;
    private final int rejectedCount;
    private final List<String> recentExecutedActions;

    public ActionFeedbackSummary(String userId, int totalExecutedActions, int acceptedCount, int rejectedCount, List<String> recentExecutedActions) {
        this.userId = userId;
        this.totalExecutedActions = totalExecutedActions;
        this.acceptedCount = acceptedCount;
        this.rejectedCount = rejectedCount;
        this.recentExecutedActions = recentExecutedActions;
    }

    public String getUserId() { return userId; }
    public int getTotalExecutedActions() { return totalExecutedActions; }
    public int getAcceptedCount() { return acceptedCount; }
    public int getRejectedCount() { return rejectedCount; }
    public List<String> getRecentExecutedActions() { return recentExecutedActions; }
}
