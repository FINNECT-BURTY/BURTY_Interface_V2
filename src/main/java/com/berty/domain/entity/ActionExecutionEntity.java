package com.berty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_action_execution")
@Getter
@Setter
@NoArgsConstructor
public class ActionExecutionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "execution_id")
    private Long executionId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "action_type", nullable = false, length = 64)
    private String actionType;

    @Column(name = "executed", nullable = false)
    private boolean executed;

    @Column(name = "message")
    private String message;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;
}
