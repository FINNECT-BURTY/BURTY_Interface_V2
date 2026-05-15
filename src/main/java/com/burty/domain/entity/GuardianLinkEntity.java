package com.burty.domain.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_guardian_link")
public class GuardianLinkEntity {
    @Id
    @Column(name = "link_id", columnDefinition = "BINARY(16)")
    private UUID linkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "senior_user_id", nullable = false)
    private UserEntity seniorUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_user_id", nullable = false)
    private UserEntity guardianUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation", nullable = false)
    private Relation relation;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false)
    private Permission permission = Permission.VIEW_ONLY;

    @Column(name = "senior_consent_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID seniorConsentId;

    @Column(name = "guardian_consent_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID guardianConsentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LinkStatus status = LinkStatus.PENDING;

    @Column(name = "linked_at")
    private LocalDateTime linkedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "revoked_by")
    private RevokedBy revokedBy;

    public enum Relation { CHILD, SPOUSE, PARENT, SIBLING, CAREGIVER }
    public enum Permission { VIEW_ONLY, VIEW_AND_ALERT }
    public enum LinkStatus { PENDING, ACTIVE, SUSPENDED, REVOKED }
    public enum RevokedBy { SENIOR, GUARDIAN, SYSTEM }
}
