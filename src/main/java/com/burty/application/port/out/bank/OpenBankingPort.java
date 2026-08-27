/**
 *
 *
 * <pre>
 * <b>Description  : 공통 포트 인터페이스 (OpenBankingPort)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.out.bank
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
package com.burty.application.port.out.bank;

import java.util.Map;

public interface OpenBankingPort {
  Map<String, Object> getAccounts(String userId);

  Map<String, Object> getBalance(String userId, String fintechUseNum);

  Map<String, Object> getTransactions(String userId, String fintechUseNum);

  /**
   * 이체를 요청한다.
   *
   * <p>결과가 불확실한 경우 (타임아웃, 5xx, 커넥션 오류) {@link
   * com.burty.core.exception.ExternalCallUnresolvedException} 을 던진다. 명확한 거절은 {@link
   * com.burty.core.exception.BusinessException} 이다. 호출자는 이 둘을 반드시 다르게 처리해야 한다.
   */
  Map<String, Object> transfer(
      String userId, String fromAccount, String toAccount, long amount, String idempotencyKey);

  /**
   * 이체 건의 최종 상태를 은행에 조회한다. 정산(reconciliation) 전용.
   *
   * @param idempotencyKey 이체 요청에 사용한 멱등키 (= 은행 거래고유번호 도출 근거)
   */
  TransferStatus getTransferStatus(String userId, String idempotencyKey);
}
