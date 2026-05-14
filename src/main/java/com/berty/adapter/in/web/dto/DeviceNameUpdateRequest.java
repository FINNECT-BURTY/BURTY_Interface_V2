package com.berty.adapter.in.web.dto;

public class DeviceNameUpdateRequest {
    private String userId;
    private String deviceName;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
}
