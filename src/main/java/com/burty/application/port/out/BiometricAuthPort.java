package com.burty.application.port.out;

public interface BiometricAuthPort {
    boolean verifyAssertion(String userId, String assertionToken);
}
