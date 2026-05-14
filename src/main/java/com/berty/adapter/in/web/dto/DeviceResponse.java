package com.berty.adapter.in.web.dto;

import java.time.LocalDateTime;

public class DeviceResponse {
    private String deviceId;
    private String deviceName;
    private String platform;
    private String osVersion;
    private String appVersion;
    private boolean trusted;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;

    public DeviceResponse() {}

    public DeviceResponse(String deviceId, String deviceName, String platform, String osVersion, String appVersion,
                          boolean trusted, LocalDateTime lastSeenAt, LocalDateTime createdAt) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.platform = platform;
        this.osVersion = osVersion;
        this.appVersion = appVersion;
        this.trusted = trusted;
        this.lastSeenAt = lastSeenAt;
        this.createdAt = createdAt;
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
    public boolean isTrusted() { return trusted; }
    public void setTrusted(boolean trusted) { this.trusted = trusted; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
