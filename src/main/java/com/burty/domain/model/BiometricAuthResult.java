package com.burty.domain.model;

public class BiometricAuthResult {
    private final String userId;
    private final String deviceId;
    private final String deviceToken;
    private final String accessToken;
    private final boolean authenticated;
    private final boolean trustedDevice;

    public BiometricAuthResult(String userId, String deviceId, String deviceToken, String accessToken,
                               boolean authenticated, boolean trustedDevice) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.deviceToken = deviceToken;
        this.accessToken = accessToken;
        this.authenticated = authenticated;
        this.trustedDevice = trustedDevice;
    }

    public String getUserId() { return userId; }
    public String getDeviceId() { return deviceId; }
    public String getDeviceToken() { return deviceToken; }
    public String getAccessToken() { return accessToken; }
    public boolean isAuthenticated() { return authenticated; }
    public boolean isTrustedDevice() { return trustedDevice; }
}
