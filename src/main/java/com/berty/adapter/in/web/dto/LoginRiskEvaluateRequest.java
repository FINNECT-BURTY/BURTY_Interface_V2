package com.berty.adapter.in.web.dto;

public class LoginRiskEvaluateRequest {
    private String userId;
    private String deviceFingerprint;
    private String ipAddress;
    private String region;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
}
