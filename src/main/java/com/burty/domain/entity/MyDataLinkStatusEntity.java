package com.burty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tbl_mydata_link_status",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_mydata_link_user_inst", columnNames = {"user_id", "institution_code"})
        },
        indexes = {
                @Index(name = "idx_mydata_link_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class MyDataLinkStatusEntity {

    @Id
    @Column(name = "link_status_id", length = 200)
    private String linkStatusId;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "institution_code", length = 64, nullable = false)
    private String institutionCode = "MYDATA";

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "linked_at")
    private LocalDateTime linkedAt;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "last_error_code", length = 64)
    private String lastErrorCode;

    @Column(name = "last_error_at")
    private LocalDateTime lastErrorAt;

    @Column(name = "unlinked_at")
    private LocalDateTime unlinkedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (linkStatusId == null) linkStatusId = userId + "|" + institutionCode;
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
