/**
 *
 *
 * <pre>
 * <b>Description  : 거래 응답 DTO (TransactionResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.dto.transaction
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
package com.burty.application.dto.transaction;

import com.burty.domain.transaction.entity.TransactionEntity;
import java.time.LocalDate;

public record TransactionResponse(
    String txId,
    LocalDate txnDate,
    long amount,
    String direction,
    String merchant,
    String memo,
    String expenseCategoryCode,
    String incomeCategoryCode,
    String source,
    Double categoryConfidence) {

  public static TransactionResponse from(TransactionEntity entity) {
    return new TransactionResponse(
        entity.getTxId() == null ? null : entity.getTxId().toString(),
        entity.getTxnDate(),
        entity.getAmount(),
        entity.getDirection(),
        entity.getMerchant(),
        entity.getMemo(),
        entity.getExpenseCategoryCode(),
        entity.getIncomeCategoryCode(),
        entity.getSource(),
        entity.getCategoryConfidence());
  }
}
