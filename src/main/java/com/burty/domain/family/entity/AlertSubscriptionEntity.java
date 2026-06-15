/**
 *
 *
 * <pre>
 * <b>Description  : 가족보호 엔티티 (AlertSubscriptionEntity)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.family.entity
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
package com.burty.domain.family.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tbl_alert_subscription")
@Getter
@Setter
@NoArgsConstructor
public class AlertSubscriptionEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "subscription_id")
  private Long subscriptionId;

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

  public enum AlertType {
    TRANSFER_OVER_AMOUNT,
    LATE_NIGHT_TRANSACTION,
    UNUSUAL_PATTERN,
    LOGIN_NEW_DEVICE,
    CONSENT_EXPIRING,
    BALANCE_BELOW_THRESHOLD
  }

  public enum Channel {
    PUSH,
    SMS,
    EMAIL,
    ALL
  }
}
