/**
 *
 *
 * <pre>
 * <b>Description  : 금융 리포지토리 (TransferOrderRepository)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.finance.repository
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
package com.burty.domain.finance.repository;

import com.burty.domain.finance.entity.TransferOrderEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferOrderRepository extends JpaRepository<TransferOrderEntity, Long> {
  List<TransferOrderEntity> findByUser_UserId(Long userId);

  Optional<TransferOrderEntity> findByIdempotencyKey(String idempotencyKey);

  Optional<TransferOrderEntity> findByUser_UserIdAndIdempotencyKey(
      Long userId, String idempotencyKey);
}
