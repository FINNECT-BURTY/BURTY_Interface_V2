package com.berty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "tbl_transaction",
        indexes = {
                @Index(name = "idx_txn_user_date", columnList = "user_id, txn_date"),
                @Index(name = "idx_txn_user_category", columnList = "user_id, expense_category_code"),
                @Index(name = "idx_txn_dedup", columnList = "user_id, external_tx_id", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TransactionEntity {

    @Id
    @Column(name = "tx_id", columnDefinition = "BINARY(16)")
    private UUID txId;

    @Column(name = "user_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID userId;

    @Column(name = "account_id", columnDefinition = "BINARY(16)")
    private UUID accountId;

    @Column(name = "external_tx_id", length = 80, nullable = false)
    private String externalTxId;

    @Column(name = "txn_date", nullable = false)
    private LocalDate txnDate;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "direction", length = 8, nullable = false)
    private String direction;

    @Column(name = "merchant", length = 120)
    private String merchant;

    @Column(name = "memo", length = 255)
    private String memo;

    @Column(name = "expense_category_code", length = 40)
    private String expenseCategoryCode;

    @Column(name = "income_category_code", length = 40)
    private String incomeCategoryCode;

    @Column(name = "source", length = 16, nullable = false)
    private String source;

    @Column(name = "category_confidence")
    private Double categoryConfidence;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (txId == null) txId = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
