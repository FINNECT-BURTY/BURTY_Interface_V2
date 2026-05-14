package com.berty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_biometric_credential")
@Getter
@Setter
@NoArgsConstructor
public class BiometricCredentialEntity {
    @Id
    @Column(name = "credential_id", columnDefinition = "BINARY(16)")
    private UUID credentialId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private DeviceEntity device;

    @Enumerated(EnumType.STRING)
    @Column(name = "credential_type", nullable = false)
    private CredentialType credentialType;

    @Column(name = "public_key", nullable = false)
    private byte[] publicKey;

    @Column(name = "credential_id_raw", nullable = false)
    private byte[] credentialIdRaw;

    @Column(name = "sign_count", nullable = false)
    private Long signCount = 0L;

    @Column(name = "aaguid", columnDefinition = "BINARY(16)")
    private UUID aaguid;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public enum CredentialType { FINGERPRINT, FACE_ID, PIN }
}
