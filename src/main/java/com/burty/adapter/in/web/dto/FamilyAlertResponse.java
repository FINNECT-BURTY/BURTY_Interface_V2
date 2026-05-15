package com.burty.adapter.in.web.dto;

import java.time.LocalDateTime;

public class FamilyAlertResponse {
    private String userId;
    private String message;
    private LocalDateTime sentAt;

    public FamilyAlertResponse() {}
    public FamilyAlertResponse(String userId, String message, LocalDateTime sentAt) {
        this.userId = userId;
        this.message = message;
        this.sentAt = sentAt;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
