package com.berty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "tbl_risk_assessment",
        indexes = {
                @Index(name = "idx_risk_user_assessed", columnList = "user_id, assessed_at"),
                @Index(name = "idx_risk_user_level", columnList = "user_id, level")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RiskAssessmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assessment_id")
    private Long assessmentId;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "level", length = 16, nullable = false)
    private String level;

    @Column(name = "threshold_amount", nullable = false)
    private Long thresholdAmount;

    @Column(name = "projected_balance", nullable = false)
    private Long projectedBalance;

    @Column(name = "risk_date")
    private LocalDate riskDate;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "assessed_at", nullable = false)
    private LocalDateTime assessedAt;

    @PrePersist
    void onCreate() {
        if (assessedAt == null) assessedAt = LocalDateTime.now();
    }
}
