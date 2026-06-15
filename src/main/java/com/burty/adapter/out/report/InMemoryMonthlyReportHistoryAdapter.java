/**
 *
 *
 * <pre>
 * <b>Description  : 외부연동 외부 연동 어댑터 (InMemoryMonthlyReportHistoryAdapter)</b>
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
import com.burty.core.constant.LogMessages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnMissingBean(MonthlyReportHistoryPort.class)
public class InMemoryMonthlyReportHistoryAdapter implements MonthlyReportHistoryPort {
  @Override
  public void saveHistory(String userId, String month, String status, String detail) {
    log.info(LogMessages.Report.HISTORY_STORED, userId, month, status, detail);
  }
}
