package com.berty.application.port.in;

import java.util.Map;

public interface ExternalFinanceUseCase {
    Map<String, Object> transferToKakaoBank(String userId, String toAccount, long amount);
    Map<String, Object> transferToHanaBank(String userId, String toAccount, long amount);
    Map<String, Object> transferToKbBank(String userId, String toAccount, long amount);
    Map<String, Object> transferToShinhanBank(String userId, String toAccount, long amount);
    Map<String, Object> transferToImBank(String userId, String toAccount, long amount);
    Map<String, Object> getOpenBankingAccounts(String userId);
    Map<String, Object> getOpenBankingBalance(String userId, String fintechUseNum);
    Map<String, Object> getOpenBankingTransactions(String userId, String fintechUseNum);
    Map<String, Object> transferByOpenBanking(String userId, String fromAccount, String toAccount, long amount, String idempotencyKey);
    Map<String, Object> getPensionSummary(String userId);
}
