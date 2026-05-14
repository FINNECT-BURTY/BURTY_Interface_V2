package com.berty.adapter.in.web.dto;

public class ActionExecutionResponse {
    private String userId;
    private String actionType;
    private boolean executed;
    private String message;

    public ActionExecutionResponse() {}

    public ActionExecutionResponse(String userId, String actionType, boolean executed, String message) {
        this.userId = userId;
        this.actionType = actionType;
        this.executed = executed;
        this.message = message;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public boolean isExecuted() { return executed; }
    public void setExecuted(boolean executed) { this.executed = executed; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
