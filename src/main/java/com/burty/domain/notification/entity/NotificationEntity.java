/**
 *
 *
 * <pre>
 * <b>Description  : 알림 엔티티 (NotificationEntity)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.notification.entity
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.domain.notification.entity;

import com.burty.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

  @Column(name = "related_entity_id")
  private Long relatedEntityId;

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

  public enum NotificationType {
    TRANSFER_ALERT,
    REPORT_READY,
    CONSENT_EXPIRING,
    GUARDIAN_REQUEST,
    BIOMETRIC_REGISTERED,
    UNUSUAL_LOGIN,
    CASHFLOW_RISK,
    CARD_DUE,
    RENT_DUE,
    POLICY_DEADLINE,
    SYSTEM
  }

  public enum Channel {
    PUSH,
    SMS,
    EMAIL,
    IN_APP
  }

  public enum Status {
    QUEUED,
    SENT,
    DELIVERED,
    READ,
    FAILED
  }
}
