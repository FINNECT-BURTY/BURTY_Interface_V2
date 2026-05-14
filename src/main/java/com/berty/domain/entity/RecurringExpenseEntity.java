package com.berty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "tbl_recurring_expense",
        indexes = {
                @Index(name = "idx_recurring_user", columnList = "user_id"),
                @Index(name = "idx_recurring_user_category", columnList = "user_id, expense_category_code")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RecurringExpenseEntity {

    @Id
    @Column(name = "recurring_id", columnDefinition = "BINARY(16)")
    private UUID recurringId;

    @Column(name = "user_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID userId;

    @Column(name = "expense_category_code", length = 40, nullable = false)
    private String expenseCategoryCode;

    @Column(name = "name", length = 80, nullable = false)
    private String name;

    @Column(name = "avg_amount", nullable = false)
    private Long avgAmount;

    @Column(name = "day_of_month", nullable = false)
    private Integer dayOfMonth;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "occurrence_count")
    private Integer occurrenceCount;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (recurringId == null) recurringId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
