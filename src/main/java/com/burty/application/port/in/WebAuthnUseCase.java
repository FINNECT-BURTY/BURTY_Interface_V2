package com.burty.application.port.in;

import com.burty.domain.model.BiometricAuthResult;

public interface WebAuthnUseCase {
    String beginRegistration(String userId);
    boolean finishRegistration(String userId, String challengeId, String attestation);
    BiometricAuthResult finishRegistration(String userId, String challengeId, String attestation,
                                           String deviceFingerprint, String platform, String biometricType);
    String beginAuthentication(String userId);
    boolean finishAuthentication(String userId, String challengeId, String assertion);
    BiometricAuthResult finishAuthentication(String userId, String challengeId, String assertion, String deviceToken);
}
