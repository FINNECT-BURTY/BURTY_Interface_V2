/**
 *
 *
 * <pre>
 * <b>Description  : 현금흐름 요청 DTO (CashflowScheduleRequest)</b>
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

public record CashflowScheduleRequest(
    String userId,
    String scheduleTypeCode,
    String label,
    Long amount,
    String direction,
    Integer dayOfMonth) {}
