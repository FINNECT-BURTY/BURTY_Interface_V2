/**
 *
 *
 * <pre>
 * <b>Description  : 거래 유스케이스 포트 (TransactionSyncUseCase)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.in.transaction
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
package com.burty.application.port.in.transaction;

import com.burty.domain.transaction.entity.TransactionEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionSyncUseCase {

  int syncFromOpenBanking(String userId, String fintechUseNum);

  List<TransactionEntity> recent(String userId, LocalDate from, LocalDate to);

  /** 페이지 단위 조회. 기간 미지정 시 최근 3개월. */
  Page<TransactionEntity> recent(String userId, LocalDate from, LocalDate to, Pageable pageable);

  int recategorizeAll(String userId);
}
