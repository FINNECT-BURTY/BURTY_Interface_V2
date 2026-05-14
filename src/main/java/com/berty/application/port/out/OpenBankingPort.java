package com.berty.application.port.out;

import java.util.Map;

public interface OpenBankingPort {
    Map<String, Object> getAccounts(String userId);
    Map<String, Object> getBalance(String userId, String fintechUseNum);
    Map<String, Object> getTransactions(String userId, String fintechUseNum);
    Map<String, Object> transfer(String userId, String fromAccount, String toAccount, long amount, String idempotencyKey);
}
