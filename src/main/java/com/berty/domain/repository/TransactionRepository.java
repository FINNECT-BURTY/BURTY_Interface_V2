package com.berty.domain.repository;

import com.berty.domain.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    List<TransactionEntity> findByUserIdAndTxnDateBetweenOrderByTxnDateDesc(UUID userId, LocalDate from, LocalDate to);

    List<TransactionEntity> findByUserIdOrderByTxnDateDesc(UUID userId);

    Optional<TransactionEntity> findByUserIdAndExternalTxId(UUID userId, String externalTxId);

    @org.springframework.data.jpa.repository.Query(
            "select distinct t.userId from TransactionEntity t where t.txnDate >= :since"
    )
    List<UUID> findDistinctUserIdsSince(@org.springframework.data.repository.query.Param("since") LocalDate since);
}
