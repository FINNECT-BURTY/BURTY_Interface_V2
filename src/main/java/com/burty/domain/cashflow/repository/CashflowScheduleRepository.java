/**
 *
 *
 * <pre>
 * <b>Description  : 현금흐름 리포지토리 (CashflowScheduleRepository)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.cashflow.repository
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
package com.burty.domain.cashflow.repository;

import com.burty.domain.cashflow.entity.CashflowScheduleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashflowScheduleRepository extends JpaRepository<CashflowScheduleEntity, Long> {
  List<CashflowScheduleEntity> findByUserIdAndActiveTrue(Long userId);
}
