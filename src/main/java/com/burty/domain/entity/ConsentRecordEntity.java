package com.burty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_consent_record")
@Getter
@Setter
@NoArgsConstructor
public class ConsentRecordEntity {
    @Id
    @Column(name = "consent_id", columnDefinition = "BINARY(16)")
    private UUID consentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false)
    private ConsentType consentType;

    @Column(name = "consent_version", nullable = false)
    private String consentVersion;

    @Column(name = "document_hash", nullable = false, length = 64)
    private String documentHash;

    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoke_reason")
    private String revokeReason;

    @Column(name = "ip_address")
    private byte[] ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    public enum ConsentType { TERMS, PRIVACY, MYDATA, FAMILY_SHARE, MARKETING, THIRD_PARTY_SHARE, SECURITY_LOG, LOCATION_POLICY_MATCH }
}
