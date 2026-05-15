package com.burty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_alert_subscription")
@Getter
@Setter
@NoArgsConstructor
public class AlertSubscriptionEntity {
    @Id
    @Column(name = "subscription_id", columnDefinition = "BINARY(16)")
    private UUID subscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id", nullable = false)
    private GuardianLinkEntity guardianLink;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    @Column(name = "threshold_amount")
    private Long thresholdAmount;

    @Column(name = "threshold_time_from")
    private LocalTime thresholdTimeFrom;

    @Column(name = "threshold_time_to")
    private LocalTime thresholdTimeTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private Channel channel = Channel.PUSH;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum AlertType { TRANSFER_OVER_AMOUNT, LATE_NIGHT_TRANSACTION, UNUSUAL_PATTERN, LOGIN_NEW_DEVICE, CONSENT_EXPIRING, BALANCE_BELOW_THRESHOLD }
    public enum Channel { PUSH, SMS, EMAIL, ALL }
}
