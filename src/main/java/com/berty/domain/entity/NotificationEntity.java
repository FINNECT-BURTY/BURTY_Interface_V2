package com.berty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_notification")
@Getter
@Setter
@NoArgsConstructor
public class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private UserEntity recipientUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private Channel channel;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "deep_link")
    private String deepLink;

    @Column(name = "related_entity_type")
    private String relatedEntityType;

    @Column(name = "related_entity_id", columnDefinition = "BINARY(16)")
    private UUID relatedEntityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.QUEUED;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "failed_reason")
    private String failedReason;

    public enum NotificationType { TRANSFER_ALERT, REPORT_READY, CONSENT_EXPIRING, GUARDIAN_REQUEST, BIOMETRIC_REGISTERED, UNUSUAL_LOGIN, CASHFLOW_RISK, CARD_DUE, RENT_DUE, POLICY_DEADLINE, SYSTEM }
    public enum Channel { PUSH, SMS, EMAIL, IN_APP }
    public enum Status { QUEUED, SENT, DELIVERED, READ, FAILED }
}
