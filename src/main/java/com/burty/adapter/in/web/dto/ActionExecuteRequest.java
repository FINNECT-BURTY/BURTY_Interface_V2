package com.burty.adapter.in.web.dto;

public class ActionExecuteRequest {
    private String userId;
    private String actionType;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
}
