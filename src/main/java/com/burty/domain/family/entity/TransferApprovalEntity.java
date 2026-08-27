package com.burty.domain.family.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 이체 보호자 승인 요청.
 *
 * <p>기존 가족 보호 기능은 이상 이체를 <b>사후에 알리기만</b> 했다. 알림이 갔을 때는 이미 돈이 나간 뒤다. 보이스피싱 피해가 실제로 발생하는 구조에서 사후 통지는
 * 피해를 막지 못한다.
 *
 * <p>여기서는 설정된 금액 이상의 이체를 <b>보류</b>하고 보호자 승인을 기다린다. 승인되면 실행하고, 거절되거나 만료되면 실행하지 않는다. 시니어 대상 금융 서비스에서
 * 이 차단 플로우가 사실상 핵심 가치다.
 */
@Entity
@Table(
    name = "tbl_transfer_approval",
    indexes = {
      @Index(name = "idx_approval_guardian", columnList = "guardian_user_id, status"),
      @Index(name = "idx_approval_order", columnList = "order_id"),
      @Index(name = "idx_approval_expiry", columnList = "status, expires_at")
    })
@Getter
@Setter
@NoArgsConstructor
public class TransferApprovalEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "approval_id")
  private Long approvalId;

  @Column(name = "order_id", nullable = false)
  private Long orderId;

  /** 이체를 요청한 사용자 (피보호자). */
  @Column(name = "requester_user_id", nullable = false, length = 64)
  private String requesterUserId;

  /** 승인 권한을 가진 보호자. */
  @Column(name = "guardian_user_id", nullable = false, length = 64)
  private String guardianUserId;

  @Column(name = "amount", nullable = false)
  private Long amount;

  @Column(name = "to_account_masked", nullable = false, length = 80)
  private String toAccountMasked;

  @Column(name = "reason", length = 200)
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private Status status = Status.PENDING;

  @Column(name = "requested_at", nullable = false)
  private LocalDateTime requestedAt;

  /** 이 시각까지 승인되지 않으면 자동 만료된다. 무한정 보류되면 사용자가 이체를 못 쓴다. */
  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "decided_at")
  private LocalDateTime decidedAt;

  @Column(name = "decision_note", length = 200)
  private String decisionNote;

  public enum Status {
    PENDING,
    APPROVED,
    REJECTED,
    EXPIRED
  }
}
