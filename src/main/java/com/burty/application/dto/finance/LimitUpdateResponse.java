/**
 *
 *
 * <pre>
 * <b>Description  : 금융 응답 DTO (LimitUpdateResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.dto.finance
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
package com.burty.application.dto.finance;

public record LimitUpdateResponse(boolean updated, long limit) {}
