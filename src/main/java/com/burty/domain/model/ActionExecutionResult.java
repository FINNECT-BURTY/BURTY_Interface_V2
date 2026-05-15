package com.burty.domain.model;

public class ActionExecutionResult {
    private final String userId;
    private final String actionType;
    private final boolean executed;
    private final String message;

    public ActionExecutionResult(String userId, String actionType, boolean executed, String message) {
        this.userId = userId;
        this.actionType = actionType;
        this.executed = executed;
        this.message = message;
    }

    public String getUserId() { return userId; }
    public String getActionType() { return actionType; }
    public boolean isExecuted() { return executed; }
    public String getMessage() { return message; }
}
