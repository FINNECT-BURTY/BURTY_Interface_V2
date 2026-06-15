/**
 *
 *
 * <pre>
 * <b>Description  : 현금흐름 유스케이스 포트 (KpiDashboardUseCase)</b>
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

import com.burty.application.dto.cashflow.GlobalKpiResponse;
import com.burty.application.dto.cashflow.UserKpiResponse;

public interface KpiDashboardUseCase {

  UserKpiResponse userKpi(String userId);

  GlobalKpiResponse globalKpi();
}
