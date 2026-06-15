/**
 *
 *
 * <pre>
 * <b>Description  : 상담 리포지토리 (MonthlyReportRepository)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.consult.repository
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
package com.burty.domain.consult.repository;

import com.burty.domain.consult.entity.MonthlyReportEntity;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyReportRepository extends JpaRepository<MonthlyReportEntity, Long> {
  Optional<MonthlyReportEntity> findByUser_UserIdAndPeriodMonth(Long userId, LocalDate periodMonth);
}
