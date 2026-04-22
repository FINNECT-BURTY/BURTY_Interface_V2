package com.nuri.application.port.in;

public interface WebAuthnUseCase {
    String beginRegistration(String userId);
    boolean finishRegistration(String userId, String challengeId, String attestation);
    String beginAuthentication(String userId);
    boolean finishAuthentication(String userId, String challengeId, String assertion);
}
