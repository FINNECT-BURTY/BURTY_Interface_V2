package com.burty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tbl_registered_account",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_registered_account_pair", columnNames = {"user_id", "account_no"})
        },
        indexes = {
                @Index(name = "idx_registered_account_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RegisteredAccountEntity {

    @Id
    @Column(name = "registered_id", length = 200)
    private String registeredId;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "account_no", length = 80, nullable = false)
    private String accountNo;

    @Column(name = "alias", length = 80)
    private String alias;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @PrePersist
    void onCreate() {
        if (registeredId == null) {
            registeredId = userId + "|" + accountNo;
        }
        if (registeredAt == null) {
            registeredAt = LocalDateTime.now();
        }
    }
}
