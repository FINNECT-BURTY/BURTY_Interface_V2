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

  /** 아직 실행되지 않은 이체를 취소한다. 이미 은행에 나간 건은 취소가 아니라 반환 절차 대상이다. */
  void cancelTransfer(String userId, String idempotencyKey, String reason);

  void updateLimit(String userId, long newLimit);

  long getLimit(String userId);

  /**
   * 이체 상세 조회.
   *
   * <p>{@code userId} 를 반드시 받는다. 예전에는 {@code transferId} 만으로 조회할 수 있어서, 인증만 된 사용자라면 남의 이체 내역을 ID
   * 추측만으로 읽을 수 있었다.
   */
  TransferResult getTransfer(String userId, String transferId);

  List<TransferResult> getTransfers(String userId);
}
