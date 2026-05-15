package com.burty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_user")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {
    @Id
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "ci_hash", nullable = false, length = 64, unique = true)
    private String ciHash;

    @Column(name = "ci_encrypted", nullable = false)
    private byte[] ciEncrypted;

    @Column(name = "phone_hash", nullable = false, length = 64, unique = true)
    private String phoneHash;

    @Column(name = "phone_encrypted", nullable = false)
    private byte[] phoneEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip")
    private byte[] lastLoginIp;

    @Column(name = "failed_login_count", nullable = false)
    private Integer failedLoginCount = 0;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum UserStatus { ACTIVE, SUSPENDED, WITHDRAWN }
}
