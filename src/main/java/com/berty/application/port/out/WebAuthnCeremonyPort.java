package com.berty.application.port.out;

import com.berty.domain.model.BiometricAuthResult;

public interface WebAuthnCeremonyPort {
    String issueChallenge(String userId, String flowType);
    boolean verifyAndConsumeChallenge(String userId, String challengeId, String signedPayload, String flowType);
    BiometricAuthResult registerTrustedDevice(String userId, String challengeId, String signedPayload,
                                              String deviceFingerprint, String platform, String biometricType);
    BiometricAuthResult authenticateTrustedDevice(String userId, String challengeId, String signedPayload, String deviceToken);
    void saveCredential(String userId, String credentialId);
}
