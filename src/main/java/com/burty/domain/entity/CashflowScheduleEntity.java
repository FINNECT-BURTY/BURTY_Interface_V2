package com.burty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tbl_cashflow_schedule",
        indexes = {
                @Index(name = "idx_cf_schedule_user", columnList = "user_id, active"),
                @Index(name = "idx_cf_schedule_user_day", columnList = "user_id, day_of_month")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CashflowScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "schedule_type_code", length = 40, nullable = false)
    private String scheduleTypeCode;

    @Column(name = "label", length = 80, nullable = false)
    private String label;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "direction", length = 8, nullable = false)
    private String direction;

    @Column(name = "day_of_month", nullable = false)
    private Integer dayOfMonth;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "source", length = 16, nullable = false)
    private String source = "USER";

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
