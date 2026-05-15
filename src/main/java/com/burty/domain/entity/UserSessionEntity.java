package com.burty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_user_session", indexes = {
        @Index(name = "idx_session_user_active", columnList = "user_id, revoked_at"),
        @Index(name = "idx_session_refresh", columnList = "refresh_token_hash")
})
@Getter
@Setter
@NoArgsConstructor
public class UserSessionEntity {
    @Id
    @Column(name = "session_id", columnDefinition = "BINARY(16)")
    private UUID sessionId;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "device_id", length = 64)
    private String deviceId;

    @Column(name = "refresh_token_hash", length = 64, nullable = false)
    private String refreshTokenHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @PrePersist
    void onCreate() {
        if (sessionId == null) sessionId = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
