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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
  List<TransactionEntity> findByUserIdAndTxnDateBetweenOrderByTxnDateDesc(
      Long userId, LocalDate from, LocalDate to);

  List<TransactionEntity> findByUserIdOrderByTxnDateDesc(Long userId);

  /**
   * 페이지 단위 조회.
   *
   * <p>예전에는 기간 내 전체를 List 로 반환했다. 3개월치가 수천 건인 사용자의 요청 하나가 그대로 힙에 올라오고 JSON 으로 직렬화됐다.
   */
  Page<TransactionEntity> findByUserIdAndTxnDateBetween(
      Long userId, LocalDate from, LocalDate to, Pageable pageable);

  /** 재분류 배치용 — 전체를 한 번에 올리지 않도록 청크 단위로 읽는다. */
  Page<TransactionEntity> findByUserId(Long userId, Pageable pageable);

  Optional<TransactionEntity> findByUserIdAndExternalTxId(Long userId, String externalTxId);

  @org.springframework.data.jpa.repository.Query(
      "select distinct t.userId from TransactionEntity t where t.txnDate >= :since")
  List<Long> findDistinctUserIdsSince(
      @org.springframework.data.repository.query.Param("since") LocalDate since);
}
