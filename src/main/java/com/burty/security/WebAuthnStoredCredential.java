package com.burty.security;

/**
 * DB에 보관된 WebAuthn 자격 증명(검증 시 COSE 공개키 재사용).
 */
public final class WebAuthnStoredCredential {
    private final byte[] credentialIdRaw;
    private final byte[] cosePublicKey;
    private final long signCount;

    public WebAuthnStoredCredential(byte[] credentialIdRaw, byte[] cosePublicKey, long signCount) {
        this.credentialIdRaw = credentialIdRaw == null ? new byte[0] : credentialIdRaw.clone();
        this.cosePublicKey = cosePublicKey == null ? new byte[0] : cosePublicKey.clone();
        this.signCount = signCount;
    }

    public byte[] getCredentialIdRaw() {
        return credentialIdRaw.clone();
    }

    public byte[] getCosePublicKey() {
        return cosePublicKey.clone();
    }

    public long getSignCount() {
        return signCount;
    }
}
