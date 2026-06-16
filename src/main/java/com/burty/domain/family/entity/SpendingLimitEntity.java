/**
 *
 *
 * <pre>
 * <b>Description  : 가족보호 엔티티 (SpendingLimitEntity)</b>
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

import com.burty.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_spending_limit")
public class SpendingLimitEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "limit_id")
  private Long limitId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Enumerated(EnumType.STRING)
  @Column(name = "period_type", nullable = false)
  private PeriodType periodType;

  @Column(name = "amount_limit", nullable = false)
  private Long amountLimit;

  @Column(name = "effective_from", nullable = false)
  private LocalDateTime effectiveFrom;

  @Column(name = "effective_to")
  private LocalDateTime effectiveTo;

  @Enumerated(EnumType.STRING)
  @Column(name = "changed_by", nullable = false)
  private ChangedBy changedBy;

  @Column(name = "change_reason")
  private String changeReason;

  public enum PeriodType {
    DAILY,
    MONTHLY,
    PER_TRANSACTION
  }

  public enum ChangedBy {
    USER,
    GUARDIAN,
    SYSTEM,
    COMPLIANCE
  }
}
