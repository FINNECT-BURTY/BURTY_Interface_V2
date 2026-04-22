package com.nuri.security;

public class StandardLikeWebAuthnVerifier implements WebAuthnAssertionVerifier {
    @Override
    public VerificationResult verifyRegistration(String payload, String expectedChallenge, String expectedOrigin, String expectedRpId, long currentSignCount) {
        boolean ok = containsStandardFields(payload, expectedChallenge, expectedOrigin, expectedRpId)
                && payload.contains("\"attestationObject\"");
        return new VerificationResult(ok, currentSignCount + 1, "cred-raw", "pub-key");
    }

    @Override
    public VerificationResult verifyAuthentication(String payload, String expectedChallenge, String expectedOrigin, String expectedRpId, long currentSignCount,
                                                   WebAuthnStoredCredential storedCredential) {
        boolean ok = containsStandardFields(payload, expectedChallenge, expectedOrigin, expectedRpId)
                && payload.contains("\"signature\"");
        return new VerificationResult(ok, currentSignCount + 1, "cred-raw", "pub-key");
    }

    private boolean containsStandardFields(String payload, String challenge, String origin, String rpId) {
        if (payload == null || payload.isBlank()) return false;
        return payload.contains("\"challenge\":\"" + challenge + "\"")
                && payload.contains("\"origin\":\"" + origin + "\"")
                && payload.contains("\"rpId\":\"" + rpId + "\"");
    }
}
