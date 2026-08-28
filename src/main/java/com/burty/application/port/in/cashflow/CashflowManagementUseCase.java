/**
 *
 *
 * <pre>
 * <b>Description  : 현금흐름 유스케이스 포트 (CashflowManagementUseCase)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.in.cashflow
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
package com.burty.application.port.in.cashflow;

import com.burty.application.dto.cashflow.CashflowCalendarDayResponse;
import com.burty.application.dto.cashflow.CashflowScheduleRequest;
import com.burty.application.dto.cashflow.CashflowScheduleResponse;
import com.burty.application.dto.cashflow.RiskCauseResponse;
import java.util.List;

public interface CashflowManagementUseCase {

  List<CashflowCalendarDayResponse> calendar(String userId);

  List<CashflowScheduleResponse> schedules(String userId);

  /**
   * @param userId 인증 토큰에서 꺼낸 사용자. 요청 본문의 userId 는 신뢰하지 않는다.
   */
  CashflowScheduleResponse upsertSchedule(String userId, CashflowScheduleRequest request);

  void deactivateSchedule(String scheduleId, String userId);

  List<RiskCauseResponse> riskCauses(String userId);
}
