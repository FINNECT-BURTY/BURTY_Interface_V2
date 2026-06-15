/**
 *
 *
 * <pre>
 * <b>Description  : 현금흐름 응답 DTO (CashflowCalendarDayResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.dto.cashflow
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
package com.burty.application.dto.cashflow;

import java.time.LocalDate;
import java.util.List;

public record CashflowCalendarDayResponse(
    LocalDate date, long expectedBalance, boolean riskDay, List<String> events) {}
