/**
 *
 *
 * <pre>
 * <b>Description  : 공통 포트 인터페이스 (MonthlyReportHistoryPort)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.out.report
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
package com.burty.application.port.out.report;

public interface MonthlyReportHistoryPort {
  void saveHistory(String userId, String month, String status, String detail);
}
