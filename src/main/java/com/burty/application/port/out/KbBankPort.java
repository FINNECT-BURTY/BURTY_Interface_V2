package com.burty.application.port.out;

import java.util.Map;

public interface KbBankPort {
    Map<String, Object> transfer(String userId, String toAccount, long amount);
}
