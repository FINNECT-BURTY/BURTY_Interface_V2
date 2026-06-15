/**
 *
 *
 * <pre>
 * <b>Description  : 거래 리포지토리 (TransactionRepository)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.transaction.repository
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.domain.transaction.repository;

import com.burty.domain.transaction.entity.TransactionEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
  List<TransactionEntity> findByUserIdAndTxnDateBetweenOrderByTxnDateDesc(
      Long userId, LocalDate from, LocalDate to);

  List<TransactionEntity> findByUserIdOrderByTxnDateDesc(Long userId);

  Optional<TransactionEntity> findByUserIdAndExternalTxId(Long userId, String externalTxId);

  @org.springframework.data.jpa.repository.Query(
      "select distinct t.userId from TransactionEntity t where t.txnDate >= :since")
  List<Long> findDistinctUserIdsSince(
      @org.springframework.data.repository.query.Param("since") LocalDate since);
}
