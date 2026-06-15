/**
 *
 *
 * <pre>
 * <b>Description  : 금융 유스케이스 포트 (ExternalFinanceUseCase)</b>
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

import com.burty.application.dto.finance.ExternalTransferResponse;
import com.burty.application.dto.finance.OpenBankingAccountsResponse;
import com.burty.application.dto.finance.OpenBankingAuthorizeResponse;
import com.burty.application.dto.finance.OpenBankingBalanceResponse;
import com.burty.application.dto.finance.OpenBankingTransactionsResponse;
import com.burty.application.dto.finance.PensionSummaryResponse;

public interface ExternalFinanceUseCase {

  ExternalTransferResponse transferToKakaoBank(String userId, String toAccount, long amount);

  ExternalTransferResponse transferToHanaBank(String userId, String toAccount, long amount);

  ExternalTransferResponse transferToKbBank(String userId, String toAccount, long amount);

  ExternalTransferResponse transferToShinhanBank(String userId, String toAccount, long amount);

  ExternalTransferResponse transferToImBank(String userId, String toAccount, long amount);

  OpenBankingAccountsResponse getOpenBankingAccounts(String userId);

  OpenBankingBalanceResponse getOpenBankingBalance(String userId, String fintechUseNum);

  OpenBankingTransactionsResponse getOpenBankingTransactions(String userId, String fintechUseNum);

  ExternalTransferResponse transferByOpenBanking(
      String userId, String fromAccount, String toAccount, long amount, String idempotencyKey);

  PensionSummaryResponse getPensionSummary(String userId);

  OpenBankingAuthorizeResponse createOpenBankingAuthorize(String userId);

  boolean exchangeOpenBankingAuthorizationCode(String userId, String code);

  boolean exchangeOpenBankingAuthorizationCodeByState(String state, String code);
}
