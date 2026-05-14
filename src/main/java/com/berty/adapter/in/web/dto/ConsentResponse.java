package com.berty.adapter.in.web.dto;

import java.time.LocalDateTime;

public class ConsentResponse {
    private String consentId;
    private String consentType;
    private String consentVersion;
    private LocalDateTime agreedAt;
    private LocalDateTime revokedAt;

    public ConsentResponse() {}

    public ConsentResponse(String consentId, String consentType, String consentVersion, LocalDateTime agreedAt, LocalDateTime revokedAt) {
        this.consentId = consentId;
        this.consentType = consentType;
        this.consentVersion = consentVersion;
        this.agreedAt = agreedAt;
        this.revokedAt = revokedAt;
    }

    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }
    public String getConsentType() { return consentType; }
    public void setConsentType(String consentType) { this.consentType = consentType; }
    public String getConsentVersion() { return consentVersion; }
    public void setConsentVersion(String consentVersion) { this.consentVersion = consentVersion; }
    public LocalDateTime getAgreedAt() { return agreedAt; }
    public void setAgreedAt(LocalDateTime agreedAt) { this.agreedAt = agreedAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
}
