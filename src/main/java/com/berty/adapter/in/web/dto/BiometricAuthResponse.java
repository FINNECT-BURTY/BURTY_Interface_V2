package com.berty.adapter.in.web.dto;

public class BiometricAuthResponse {
    private String userId;
    private String deviceId;
    private String deviceToken;
    private String accessToken;
    private String riskProof;
    private boolean authenticated;
    private boolean trustedDevice;

    public BiometricAuthResponse() {}

    public BiometricAuthResponse(String userId, String deviceId, String deviceToken, String accessToken,
                                 String riskProof, boolean authenticated, boolean trustedDevice) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.deviceToken = deviceToken;
        this.accessToken = accessToken;
        this.riskProof = riskProof;
        this.authenticated = authenticated;
        this.trustedDevice = trustedDevice;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRiskProof() { return riskProof; }
    public void setRiskProof(String riskProof) { this.riskProof = riskProof; }
    public boolean isAuthenticated() { return authenticated; }
    public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }
    public boolean isTrustedDevice() { return trustedDevice; }
    public void setTrustedDevice(boolean trustedDevice) { this.trustedDevice = trustedDevice; }
}
