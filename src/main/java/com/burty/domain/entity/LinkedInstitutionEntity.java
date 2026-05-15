package com.burty.domain.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_linked_institution")
public class LinkedInstitutionEntity {
    @Id
    @Column(name = "link_id", columnDefinition = "BINARY(16)")
    private UUID linkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "institution_code", nullable = false)
    private String institutionCode;

    @Column(name = "institution_name", nullable = false)
    private String institutionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "institution_type", nullable = false)
    private InstitutionType institutionType;

    @Column(name = "access_token_encrypted", nullable = false)
    private byte[] accessTokenEncrypted;

    @Column(name = "refresh_token_encrypted", nullable = false)
    private byte[] refreshTokenEncrypted;

    @Column(name = "token_expires_at", nullable = false)
    private LocalDateTime tokenExpiresAt;

    @Column(name = "consent_expires_at", nullable = false)
    private LocalDateTime consentExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LinkStatus status = LinkStatus.ACTIVE;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "last_error_code")
    private String lastErrorCode;

    @Column(name = "last_error_at")
    private LocalDateTime lastErrorAt;

    public enum InstitutionType { BANK, CARD, SECURITIES, PENSION, INSURANCE, P2P, CAPITAL }
    public enum LinkStatus { ACTIVE, EXPIRED, REVOKED, ERROR }
}
