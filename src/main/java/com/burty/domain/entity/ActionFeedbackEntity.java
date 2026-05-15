package com.burty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_action_feedback")
@Getter
@Setter
@NoArgsConstructor
public class ActionFeedbackEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long feedbackId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "action_type", nullable = false, length = 64)
    private String actionType;

    @Column(name = "feedback", nullable = false, length = 100)
    private String feedback;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
