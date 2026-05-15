package com.burty.adapter.in.web.dto;

import java.time.LocalDateTime;

public class SessionResponse {
    private String sessionId;
    private String userId;
    private String deviceId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public SessionResponse() {}

    public SessionResponse(String sessionId, String userId, String deviceId, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.deviceId = deviceId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
