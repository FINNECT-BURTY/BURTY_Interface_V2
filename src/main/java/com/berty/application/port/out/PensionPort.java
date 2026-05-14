package com.berty.application.port.out;

import java.util.Map;

public interface PensionPort {
    Map<String, Object> getSummary(String userId);
}
