package com.burty.domain.repository;

import com.burty.domain.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    List<TransactionEntity> findByUserIdAndTxnDateBetweenOrderByTxnDateDesc(Long userId, LocalDate from, LocalDate to);

    List<TransactionEntity> findByUserIdOrderByTxnDateDesc(Long userId);

    Optional<TransactionEntity> findByUserIdAndExternalTxId(Long userId, String externalTxId);

    @org.springframework.data.jpa.repository.Query(
            "select distinct t.userId from TransactionEntity t where t.txnDate >= :since"
    )
    List<Long> findDistinctUserIdsSince(@org.springframework.data.repository.query.Param("since") LocalDate since);
}
