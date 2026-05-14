package com.berty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tbl_social_account",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_social_provider_subject", columnNames = {"provider", "provider_user_id_hash"})
        },
        indexes = {
                @Index(name = "idx_social_user", columnList = "user_id"),
                @Index(name = "idx_social_email", columnList = "email_hash")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class SocialAccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_account_id")
    private Long socialAccountId;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "provider", length = 20, nullable = false)
    private String provider;

    @Column(name = "provider_user_id_hash", length = 64, nullable = false)
    private String providerUserIdHash;

    @Column(name = "email_hash", length = 64)
    private String emailHash;

    @Column(name = "email_encrypted", length = 500)
    private String emailEncrypted;

    @Column(name = "display_name_encrypted", length = 500)
    private String displayNameEncrypted;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (linkedAt == null) linkedAt = now;
        if (lastLoginAt == null) lastLoginAt = now;
    }
}
