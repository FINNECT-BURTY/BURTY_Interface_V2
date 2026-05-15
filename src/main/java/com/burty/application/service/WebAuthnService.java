package com.burty.application.service;

import com.burty.application.port.in.WebAuthnUseCase;
import com.burty.application.port.out.WebAuthnCeremonyPort;
import com.burty.domain.model.BiometricAuthResult;
import org.springframework.stereotype.Service;

@Service
public class WebAuthnService implements WebAuthnUseCase {
    private final WebAuthnCeremonyPort webAuthnCeremonyPort;

    public WebAuthnService(WebAuthnCeremonyPort webAuthnCeremonyPort) {
        this.webAuthnCeremonyPort = webAuthnCeremonyPort;
    }

    @Override
    public String beginRegistration(String userId) {
        return webAuthnCeremonyPort.issueChallenge(userId, "REGISTRATION");
    }

    @Override
    public boolean finishRegistration(String userId, String challengeId, String attestation) {
        boolean verified = webAuthnCeremonyPort.verifyAndConsumeChallenge(userId, challengeId, attestation, "REGISTRATION");
        if (verified) {
            webAuthnCeremonyPort.saveCredential(userId, "cred-" + userId);
        }
        return verified;
    }

    @Override
    public BiometricAuthResult finishRegistration(String userId, String challengeId, String attestation,
                                                 String deviceFingerprint, String platform, String biometricType) {
        return webAuthnCeremonyPort.registerTrustedDevice(userId, challengeId, attestation, deviceFingerprint, platform, biometricType);
    }

    @Override
    public String beginAuthentication(String userId) {
        return webAuthnCeremonyPort.issueChallenge(userId, "AUTHENTICATION");
    }

    @Override
    public boolean finishAuthentication(String userId, String challengeId, String assertion) {
        return webAuthnCeremonyPort.verifyAndConsumeChallenge(userId, challengeId, assertion, "AUTHENTICATION");
    }

    @Override
    public BiometricAuthResult finishAuthentication(String userId, String challengeId, String assertion, String deviceToken) {
        return webAuthnCeremonyPort.authenticateTrustedDevice(userId, challengeId, assertion, deviceToken);
    }
}
