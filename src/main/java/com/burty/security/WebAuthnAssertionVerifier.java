package com.burty.security;

public interface WebAuthnAssertionVerifier {
    VerificationResult verifyRegistration(String payload, String expectedChallenge, String expectedOrigin, String expectedRpId, long currentSignCount);
    VerificationResult verifyAuthentication(String payload, String expectedChallenge, String expectedOrigin, String expectedRpId, long currentSignCount,
                                            WebAuthnStoredCredential storedCredential);

    class VerificationResult {
        private final boolean verified;
        private final long nextSignCount;
        private final String credentialIdRaw;
        private final String publicKey;

        public VerificationResult(boolean verified, long nextSignCount, String credentialIdRaw, String publicKey) {
            this.verified = verified;
            this.nextSignCount = nextSignCount;
            this.credentialIdRaw = credentialIdRaw;
            this.publicKey = publicKey;
        }

        public boolean isVerified() { return verified; }
        public long getNextSignCount() { return nextSignCount; }
        public String getCredentialIdRaw() { return credentialIdRaw; }
        public String getPublicKey() { return publicKey; }
    }
}
