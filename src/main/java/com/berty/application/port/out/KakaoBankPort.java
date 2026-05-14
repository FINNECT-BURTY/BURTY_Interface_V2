package com.berty.application.port.out;

import java.util.Map;

public interface KakaoBankPort {
    Map<String, Object> transfer(String userId, String toAccount, long amount);
}
