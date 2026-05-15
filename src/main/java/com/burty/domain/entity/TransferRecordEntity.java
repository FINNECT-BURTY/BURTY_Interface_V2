package com.burty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tbl_transfer_record",
        indexes = {
                @Index(name = "idx_transfer_record_user", columnList = "user_id, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TransferRecordEntity {

    @Id
    @Column(name = "transfer_id", length = 80)
    private String transferId;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "from_account", length = 80)
    private String fromAccount;

    @Column(name = "to_account", length = 80)
    private String toAccount;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "family_notified", nullable = false)
    private Boolean familyNotified = false;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
