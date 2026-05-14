package com.berty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tbl_policy_match_log",
        indexes = {
                @Index(name = "idx_policy_match_user", columnList = "user_id, matched_at"),
                @Index(name = "idx_policy_match_policy", columnList = "policy_code")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PolicyMatchLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_log_id")
    private Long matchLogId;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "policy_code", length = 64, nullable = false)
    private String policyCode;

    @Column(name = "policy_title", length = 200)
    private String policyTitle;

    @Column(name = "priority_score", nullable = false)
    private Integer priorityScore;

    @Column(name = "rank_in_match", nullable = false)
    private Integer rankInMatch;

    @Column(name = "occupation_code", length = 40)
    private String occupationCode;

    @Column(name = "applied", nullable = false)
    private Boolean applied = false;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "matched_at", nullable = false)
    private LocalDateTime matchedAt;

    @PrePersist
    void onCreate() {
        if (matchedAt == null) matchedAt = LocalDateTime.now();
    }
}
