package com.burty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "tbl_policy",
        indexes = {
                @Index(name = "idx_policy_active", columnList = "active, valid_from, valid_to"),
                @Index(name = "idx_policy_type", columnList = "policy_type_code")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PolicyEntity {

    @Id
    @Column(name = "policy_code", length = 64, nullable = false)
    private String policyCode;

    @Column(name = "policy_type_code", length = 40, nullable = false)
    private String policyTypeCode;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "support_type", length = 40)
    private String supportType;

    @Column(name = "age_min")
    private Integer ageMin;

    @Column(name = "age_max")
    private Integer ageMax;

    @Column(name = "income_max")
    private Long incomeMax;

    @Column(name = "occupation_code", length = 40)
    private String occupationCode;

    @Column(name = "residence_code", length = 40)
    private String residenceCode;

    @Column(name = "benefit_summary", length = 500)
    private String benefitSummary;

    @Column(name = "apply_url", length = 500)
    private String applyUrl;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "priority_base")
    private Integer priorityBase;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
