/**
 *
 *
 * <pre>
 * <b>Description  : 금융 요청 DTO (TransferRequest)</b>
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

public record TransferRequest(
    String userId,
    String fromAccount,
    String toAccount,
    long amount,
    String description,
    String assertionToken,
    String idempotencyKey) {}
