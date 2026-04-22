package com.nuri.application.port.out;

import java.util.Map;

public interface ShinhanBankPort {
    Map<String, Object> transfer(String userId, String toAccount, long amount);
}
