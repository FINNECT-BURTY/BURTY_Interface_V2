package com.berty.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "berty.webauthn")
public class WebAuthnProperties {
    private String serverSecret = "change-me-webauthn-secret";
    private long challengeTtlSeconds = 300;
    private String rpId = "localhost";
    private String origin = "http://localhost:8080";

    public String getServerSecret() { return serverSecret; }
    public void setServerSecret(String serverSecret) { this.serverSecret = serverSecret; }
    public long getChallengeTtlSeconds() { return challengeTtlSeconds; }
    public void setChallengeTtlSeconds(long challengeTtlSeconds) { this.challengeTtlSeconds = challengeTtlSeconds; }
    public String getRpId() { return rpId; }
    public void setRpId(String rpId) { this.rpId = rpId; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
}
