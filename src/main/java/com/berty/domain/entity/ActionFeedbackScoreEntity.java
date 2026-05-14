package com.berty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tbl_action_feedback_score",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_feedback_score_user_action", columnNames = {"user_id", "action_type_code"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ActionFeedbackScoreEntity {

    @Id
    @Column(name = "score_id", length = 100)
    private String scoreId;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "action_type_code", length = 40, nullable = false)
    private String actionTypeCode;

    @Column(name = "accept_count", nullable = false)
    private Integer acceptCount = 0;

    @Column(name = "reject_count", nullable = false)
    private Integer rejectCount = 0;

    @Column(name = "score", nullable = false)
    private Integer score = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void onTouch() {
        if (scoreId == null) scoreId = userId + "|" + actionTypeCode;
        updatedAt = LocalDateTime.now();
    }
}
