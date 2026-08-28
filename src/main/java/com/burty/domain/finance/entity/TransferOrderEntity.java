/**
 *
 *
 * <pre>
 * <b>Description  : 금융 엔티티 (TransferOrderEntity)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.finance.entity
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
package com.burty.domain.finance.entity;

import com.burty.domain.auth.entity.BiometricCredentialEntity;
import com.burty.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "tbl_transfer_order",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_transfer_order_idempotency",
            columnNames = {"user_id", "idempotency_key"}),
    indexes = {
      @Index(name = "idx_transfer_order_user", columnList = "user_id, order_id"),
      @Index(name = "idx_transfer_order_reconcile", columnList = "status, next_reconcile_at")
    })
@Getter
@Setter
@NoArgsConstructor
public class TransferOrderEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "order_id")
  private Long orderId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Column(name = "idempotency_key", nullable = false, length = 64)
  private String idempotencyKey;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "from_account_id")
  private AccountEntity fromAccount;

  @Column(name = "to_account_no", nullable = false, length = 80)
  private String toAccountNo;

  @Column(name = "to_account_no_masked", nullable = false, length = 80)
  private String toAccountNoMasked;

  @Column(name = "to_bank_code", nullable = false)
  private String toBankCode;

  @Column(name = "to_holder_name")
  private String toHolderName;

  @Column(name = "amount", nullable = false)
  private Long amount;

  @Column(name = "memo")
  private String memo;

  @Enumerated(EnumType.STRING)
  @Column(name = "purpose", nullable = false)
  private Purpose purpose = Purpose.TRANSFER;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status = Status.PENDING;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "biometric_credential_id")
  private BiometricCredentialEntity biometricCredential;

  @Column(name = "bank_transaction_id")
  private String bankTransactionId;

  @Column(name = "scheduled_at")
  private LocalDateTime scheduledAt;

  @Column(name = "executed_at")
  private LocalDateTime executedAt;

  @Column(name = "failed_reason")
  private String failedReason;

  /** 요청 시각. 정산 배치가 오래된 미결 건을 식별할 때 쓴다. */
  @Column(name = "requested_at", nullable = false)
  private LocalDateTime requestedAt;

  /** UNKNOWN 건의 다음 정산 조회 예정 시각. */
  @Column(name = "next_reconcile_at")
  private LocalDateTime nextReconcileAt;

  @Column(name = "reconcile_attempts", nullable = false)
  private Integer reconcileAttempts = 0;

  /**
   * 일일 한도를 실제로 차감한 사용량 행의 날짜.
   *
   * <p>해제할 때 날짜를 다시 계산하면 안 된다. 주문 생성과 한도 예약 사이에 자정이 지나면 {@code requestedAt} 기준 날짜와 실제 차감한 날짜가 달라져,
   * 엉뚱한 행을 해제하려다 실패한다. 돈이 새지는 않지만 사용자 한도가 하루 동안 복구되지 않는다.
   *
   * <p>예약 전 단계에서 실패한 주문은 null 이다 (차감한 적이 없으므로 해제할 것도 없다).
   */
  @Column(name = "limit_usage_date")
  private java.time.LocalDate limitUsageDate;

  public enum Purpose {
    SELF,
    TRANSFER,
    PAYMENT,
    INVESTMENT
  }

  public enum Status {
    /** 멱등키 선점 완료. 아직 아무것도 하지 않음. */
    PENDING,
    /** 보호자 사전 승인 대기. 이 상태에서는 은행 호출이 일어나지 않는다. */
    AWAITING_APPROVAL,
    AUTH_REQUESTED,
    AUTHORIZED,
    /** 은행에 요청을 보냈고 응답 대기 중. 이 상태로 남아 있으면 프로세스가 죽은 것이므로 정산 대상이다. */
    EXECUTING,
    EXECUTED,
    FAILED,
    CANCELLED,
    /**
     * 은행 응답을 확인하지 못함 — <b>출금됐을 수도 있다.</b> 절대 FAILED 로 뭉뚱그리면 안 되는 상태다. 정산 배치가 은행에 조회해 EXECUTED /
     * FAILED 로 확정한다.
     */
    UNKNOWN,
    /** 실행 후 취소·반환 처리됨. */
    REVERSED;

    /** 정산 배치가 결과를 확정해야 하는 상태인가. */
    public boolean needsReconciliation() {
      return this == UNKNOWN || this == EXECUTING;
    }

    /** 사용자가 취소할 수 있는 상태인가. 이미 은행에 요청이 나간 뒤에는 취소가 아니라 반환 절차다. */
    public boolean isCancellable() {
      return this == PENDING
          || this == AWAITING_APPROVAL
          || this == AUTH_REQUESTED
          || this == AUTHORIZED;
    }

    /** 더 이상 상태가 바뀌지 않는가. */
    public boolean isTerminal() {
      return this == EXECUTED || this == FAILED || this == CANCELLED || this == REVERSED;
    }
  }
}
