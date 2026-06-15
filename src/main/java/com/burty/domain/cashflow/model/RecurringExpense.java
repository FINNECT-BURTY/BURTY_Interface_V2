/**
 *
 *
 * <pre>
 * <b>Description  : 현금흐름 도메인 모델 (RecurringExpense)</b>
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

public record RecurringExpense(String name, long amount, int dayOfMonth) {}
