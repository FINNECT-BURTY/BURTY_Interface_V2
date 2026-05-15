package com.burty.domain.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_account_snapshot")
public class AccountSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private Long snapshotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Column(name = "as_of_date", nullable = false)
    private LocalDate asOfDate;

    @Column(name = "balance", nullable = false)
    private Long balance;

    @Column(name = "available_balance")
    private Long availableBalance;

    @Column(name = "holdings", columnDefinition = "JSON")
    private String holdings;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;
}
