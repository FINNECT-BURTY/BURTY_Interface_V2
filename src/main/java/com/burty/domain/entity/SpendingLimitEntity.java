package com.burty.domain.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_spending_limit")
public class SpendingLimitEntity {
    @Id
    @Column(name = "limit_id", columnDefinition = "BINARY(16)")
    private UUID limitId;

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

    public enum PeriodType { DAILY, MONTHLY, PER_TRANSACTION }
    public enum ChangedBy { USER, GUARDIAN, SYSTEM, COMPLIANCE }
}
