/**
 *
 *
 * <pre>
 * <b>Description  : 현금흐름 도메인 모델 (RiskAssessment)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.cashflow.model
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
package com.burty.domain.cashflow.model;

import java.time.LocalDate;

public record RiskAssessment(
    String userId,
    String level,
    long threshold,
    String reason,
    LocalDate riskDate,
    long projectedBalance) {}
