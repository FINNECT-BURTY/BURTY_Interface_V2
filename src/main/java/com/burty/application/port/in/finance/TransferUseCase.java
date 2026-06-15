/**
 *
 *
 * <pre>
 * <b>Description  : 금융 유스케이스 포트 (TransferUseCase)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.in.finance
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
package com.burty.application.port.in.finance;

import com.burty.domain.finance.model.TransferResult;
import java.util.List;

public interface TransferUseCase {

  TransferResult transfer(
      String userId,
      String fromAccount,
      String toAccount,
      long amount,
      String description,
      String assertionToken,
      String idempotencyKey);

  void updateLimit(String userId, long newLimit);

  long getLimit(String userId);

  TransferResult getTransfer(String transferId);

  List<TransferResult> getTransfers(String userId);
}
