package com.burty.domain.model;

import java.time.LocalDateTime;

public class FamilyAlert {
    private String userId;
    private String message;
    private LocalDateTime sentAt;

    public FamilyAlert(String userId, String message, LocalDateTime sentAt) {
        this.userId = userId;
        this.message = message;
        this.sentAt = sentAt;
    }

    public String getUserId() { return userId; }
    public String getMessage() { return message; }
    public LocalDateTime getSentAt() { return sentAt; }
}
