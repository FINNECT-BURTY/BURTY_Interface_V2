package com.nuri.application.port.in;

import java.util.Map;

public interface ExternalFinanceUseCase {
    Map<String, Object> transferToKakaoBank(String userId, String toAccount, long amount);
    Map<String, Object> transferToHanaBank(String userId, String toAccount, long amount);
    Map<String, Object> transferToKbBank(String userId, String toAccount, long amount);
    Map<String, Object> transferToShinhanBank(String userId, String toAccount, long amount);
    Map<String, Object> transferToImBank(String userId, String toAccount, long amount);
    Map<String, Object> getPensionSummary(String userId);
}
