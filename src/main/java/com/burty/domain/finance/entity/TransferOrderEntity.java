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
@Table(name = "tbl_transfer_order")
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

  public enum Purpose {
    SELF,
    TRANSFER,
    PAYMENT,
    INVESTMENT
  }

  public enum Status {
    PENDING,
    AUTH_REQUESTED,
    AUTHORIZED,
    EXECUTING,
    EXECUTED,
    FAILED,
    CANCELLED
  }
}
