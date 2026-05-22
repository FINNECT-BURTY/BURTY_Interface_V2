package com.burty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_linked_institution")
@Getter
@Setter
@NoArgsConstructor
public class LinkedInstitutionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "link_id")
    private Long linkId;

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

    @Column(name = "access_token", nullable = false, length = 2000)
    private String accessToken;

    @Column(name = "refresh_token", nullable = false, length = 2000)
    private String refreshToken;

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
