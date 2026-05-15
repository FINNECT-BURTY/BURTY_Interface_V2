package com.burty.adapter.in.web.dto;

public class UserFeedbackRequest {
    private String userId;
    private String targetType;
    private String targetId;
    private String feedbackType;
    private String feedbackValue;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getFeedbackType() { return feedbackType; }
    public void setFeedbackType(String feedbackType) { this.feedbackType = feedbackType; }
    public String getFeedbackValue() { return feedbackValue; }
    public void setFeedbackValue(String feedbackValue) { this.feedbackValue = feedbackValue; }
}
