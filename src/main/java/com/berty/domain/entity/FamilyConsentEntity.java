package com.berty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tbl_family_consent",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_family_consent_pair", columnNames = {"parent_user_id", "child_user_id"})
        },
        indexes = {
                @Index(name = "idx_family_consent_parent", columnList = "parent_user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class FamilyConsentEntity {

    @Id
    @Column(name = "consent_pair_id", length = 200)
    private String consentPairId;

    @Column(name = "parent_user_id", length = 64, nullable = false)
    private String parentUserId;

    @Column(name = "child_user_id", length = 64, nullable = false)
    private String childUserId;

    @Column(name = "consented", nullable = false)
    private Boolean consented = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (consentPairId == null) consentPairId = parentUserId + "|" + childUserId;
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
