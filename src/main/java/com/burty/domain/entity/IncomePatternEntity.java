package com.burty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(
        name = "tbl_income_pattern",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_income_user_month", columnNames = {"user_id", "period_yyyymm"})
        },
        indexes = {
                @Index(name = "idx_income_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class IncomePatternEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pattern_id")
    private Long patternId;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "period_yyyymm", length = 7, nullable = false)
    private String periodYyyymm;

    @Column(name = "total_income", nullable = false)
    private Long totalIncome;

    @Column(name = "income_count", nullable = false)
    private Integer incomeCount;

    @Column(name = "avg_income")
    private Long avgIncome;

    @Column(name = "stddev")
    private Double stddev;

    @Column(name = "min_income")
    private Long minIncome;

    @Column(name = "max_income")
    private Long maxIncome;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

    public void setPeriod(YearMonth period) {
        this.periodYyyymm = period.toString();
    }

    @PrePersist
    void onCreate() {
        if (computedAt == null) computedAt = LocalDateTime.now();
    }
}
