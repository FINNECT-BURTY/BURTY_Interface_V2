package com.burty.adapter.in.web.dto;

import java.time.LocalDateTime;

public class NotificationResponse {
    private Long notificationId;
    private String notificationType;
    private String channel;
    private String title;
    private String body;
    private String deepLink;
    private String status;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;

    public NotificationResponse() {}

    public NotificationResponse(Long notificationId, String notificationType, String channel, String title, String body,
                                String deepLink, String status, LocalDateTime sentAt, LocalDateTime readAt) {
        this.notificationId = notificationId;
        this.notificationType = notificationType;
        this.channel = channel;
        this.title = title;
        this.body = body;
        this.deepLink = deepLink;
        this.status = status;
        this.sentAt = sentAt;
        this.readAt = readAt;
    }

    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }
    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getDeepLink() { return deepLink; }
    public void setDeepLink(String deepLink) { this.deepLink = deepLink; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
