package com.nuri.application.port.out;

public interface WebAuthnCeremonyPort {
    String issueChallenge(String userId, String flowType);
    boolean verifyAndConsumeChallenge(String userId, String challengeId, String signedPayload, String flowType);
    void saveCredential(String userId, String credentialId);
}
