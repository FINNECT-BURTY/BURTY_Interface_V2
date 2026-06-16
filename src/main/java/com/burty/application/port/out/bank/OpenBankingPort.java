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

  Map<String, Object> transfer(
      String userId, String fromAccount, String toAccount, long amount, String idempotencyKey);
}
