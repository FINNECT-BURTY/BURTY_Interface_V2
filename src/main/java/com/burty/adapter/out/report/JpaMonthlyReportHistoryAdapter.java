/**
 *
 *
 * <pre>
 * <b>Description  : 외부연동 외부 연동 어댑터 (JpaMonthlyReportHistoryAdapter)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.out.report
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
package com.burty.adapter.out.report;

import com.burty.application.port.out.report.MonthlyReportHistoryPort;
import com.burty.domain.consult.entity.MonthlyReportEntity;
import com.burty.domain.consult.repository.MonthlyReportRepository;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class JpaMonthlyReportHistoryAdapter implements MonthlyReportHistoryPort {
  private final MonthlyReportRepository monthlyReportRepository;
  private final UserRepository userRepository;

  public JpaMonthlyReportHistoryAdapter(
      MonthlyReportRepository monthlyReportRepository, UserRepository userRepository) {
    this.monthlyReportRepository = monthlyReportRepository;
    this.userRepository = userRepository;
  }

  @Override
  public void saveHistory(String userId, String month, String status, String detail) {
    Long userKey = parseUserKey(userId);
    if (userKey == null) return;

    Optional<UserEntity> userOpt = userRepository.findById(userKey);
    if (userOpt.isEmpty()) return;

    YearMonth ym = YearMonth.parse(month);
    LocalDate periodMonth = ym.atDay(1);

    MonthlyReportEntity entity =
        monthlyReportRepository
            .findByUser_UserIdAndPeriodMonth(userKey, periodMonth)
            .orElseGet(MonthlyReportEntity::new);
    entity.setUser(userOpt.get());
    entity.setPeriodMonth(periodMonth);
    entity.setStatus(toStatus(status));
    entity.setFailedReason("FAILED".equalsIgnoreCase(status) ? detail : null);
    if ("SUCCESS".equalsIgnoreCase(status)) {
      entity.setDeliveredAt(java.time.LocalDateTime.now());
    }
    monthlyReportRepository.save(entity);
  }

  private MonthlyReportEntity.ReportStatus toStatus(String status) {
    if ("SUCCESS".equalsIgnoreCase(status)) return MonthlyReportEntity.ReportStatus.DELIVERED;
    if ("FAILED".equalsIgnoreCase(status)) return MonthlyReportEntity.ReportStatus.FAILED;
    return MonthlyReportEntity.ReportStatus.READY;
  }

  private Long parseUserKey(String userId) {
    try {
      return Long.parseLong(userId);
    } catch (Exception ignored) {
      return null;
    }
  }
}
