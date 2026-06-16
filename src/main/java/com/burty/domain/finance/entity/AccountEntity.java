/**
 *
 *
 * <pre>
 * <b>Description  : 금융 엔티티 (AccountEntity)</b>
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

import com.burty.domain.mydata.entity.LinkedInstitutionEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tbl_account")
@Getter
@Setter
@NoArgsConstructor
public class AccountEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "account_id")
  private Long accountId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "link_id", nullable = false)
  private LinkedInstitutionEntity linkedInstitution;

  @Column(name = "account_no", nullable = false, length = 80)
  private String accountNo;

  @Column(name = "account_no_hash", nullable = false, length = 64)
  private String accountNoHash;

  @Column(name = "account_no_masked", nullable = false, length = 80)
  private String accountNoMasked;

  @Column(name = "account_name")
  private String accountName;

  @Enumerated(EnumType.STRING)
  @Column(name = "account_type", nullable = false)
  private AccountType accountType;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency = "KRW";

  @Column(name = "is_primary", nullable = false)
  private Boolean isPrimary = false;

  @Column(name = "first_synced_at", nullable = false)
  private LocalDateTime firstSyncedAt;

  @Column(name = "last_balance")
  private Long lastBalance;

  @Column(name = "last_balance_at")
  private LocalDateTime lastBalanceAt;

  @Column(name = "closed_at")
  private LocalDateTime closedAt;

  public enum AccountType {
    DEPOSIT,
    SAVINGS,
    CHECKING,
    STOCK,
    FUND,
    PENSION,
    LOAN,
    CARD
  }
}
