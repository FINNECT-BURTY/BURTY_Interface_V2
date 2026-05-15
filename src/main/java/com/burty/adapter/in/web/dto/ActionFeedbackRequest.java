package com.burty.adapter.in.web.dto;

public class ActionFeedbackRequest {
    private String userId;
    private String actionType;
    private String feedback;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
}
