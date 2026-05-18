package com.burty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tbl_oauth_state",
        indexes = {
                @Index(name = "idx_oauth_state_expires", columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class OAuthStateEntity {

    @Id
    @Column(name = "state_key", length = 128)
    private String stateKey;

    @Column(name = "provider", length = 20, nullable = false)
    private String provider;

    /** 로그인 완료 후 redirect 할 프론트 origin. */
    @Column(name = "frontend_origin", length = 255)
    private String frontendOrigin;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
