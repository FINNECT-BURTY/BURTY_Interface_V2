package com.burty.adapter.in.web.dto;

public class WebAuthnFinishRequest {
    private String userId;
    private String challengeId;
    private String payload;
    private String deviceFingerprint;
    private String platform;
    private String biometricType;
    private String deviceToken;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getBiometricType() { return biometricType; }
    public void setBiometricType(String biometricType) { this.biometricType = biometricType; }
    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }
}
