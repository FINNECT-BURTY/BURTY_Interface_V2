package com.burty.adapter.in.web.dto;

public class ReminderGenerateResponse {
    private String userId;
    private int createdCount;

    public ReminderGenerateResponse() {}

    public ReminderGenerateResponse(String userId, int createdCount) {
        this.userId = userId;
        this.createdCount = createdCount;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getCreatedCount() { return createdCount; }
    public void setCreatedCount(int createdCount) { this.createdCount = createdCount; }
}
