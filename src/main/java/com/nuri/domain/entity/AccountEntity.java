package com.nuri.domain.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_account")
public class AccountEntity {
    @Id
    @Column(name = "account_id", columnDefinition = "BINARY(16)")
    private UUID accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id", nullable = false)
    private LinkedInstitutionEntity linkedInstitution;

    @Column(name = "account_no_encrypted", nullable = false)
    private byte[] accountNoEncrypted;

    @Column(name = "account_no_hash", nullable = false, length = 64)
    private String accountNoHash;

    @Column(name = "account_no_masked", nullable = false)
    private String accountNoMasked;

    @Column(name = "account_name")
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "KRW";

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @Column(name = "first_synced_at", nullable = false)
    private LocalDateTime firstSyncedAt;

    @Column(name = "last_balance")
    private Long lastBalance;

    @Column(name = "last_balance_at")
    private LocalDateTime lastBalanceAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    public enum AccountType { DEPOSIT, SAVINGS, CHECKING, STOCK, FUND, PENSION, LOAN, CARD }
}
