package com.burty.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "burty.notify")
public class NotifyProperties {

  private boolean asyncEnabled = true;
  private final ChannelConfig email = new ChannelConfig();
  private final SmsConfig sms = new SmsConfig();
  private final PushConfig push = new PushConfig();

  public boolean isAsyncEnabled() {
    return asyncEnabled;
  }

  public void setAsyncEnabled(boolean asyncEnabled) {
    this.asyncEnabled = asyncEnabled;
  }

  public ChannelConfig getEmail() {
    return email;
  }

  public SmsConfig getSms() {
    return sms;
  }

  public PushConfig getPush() {
    return push;
  }

  public static class ChannelConfig {
    private boolean stubMode = true;
    private String fromAddress = "noreply@burty.co.kr";
    private String fromName = "BURTY";

    public boolean isStubMode() {
      return stubMode;
    }

    public void setStubMode(boolean stubMode) {
      this.stubMode = stubMode;
    }

    public String getFromAddress() {
      return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
      this.fromAddress = fromAddress;
    }

    public String getFromName() {
      return fromName;
    }

    public void setFromName(String fromName) {
      this.fromName = fromName;
    }
  }

  public static class SmsConfig extends ChannelConfig {
    private String apiUrl = "https://api.solapi.com/messages/v4/send";
    private String apiKey = "";
    private String apiSecret = "";
    private String senderNumber = "";

    public String getApiUrl() {
      return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
      this.apiUrl = apiUrl;
    }

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    public String getApiSecret() {
      return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
      this.apiSecret = apiSecret;
    }

    public String getSenderNumber() {
      return senderNumber;
    }

    public void setSenderNumber(String senderNumber) {
      this.senderNumber = senderNumber;
    }

    public boolean isConfigured() {
      return apiKey != null
          && !apiKey.isBlank()
          && apiSecret != null
          && !apiSecret.isBlank()
          && senderNumber != null
          && !senderNumber.isBlank();
    }
  }

  public static class PushConfig extends ChannelConfig {
    private String fcmProjectId = "";
    private String fcmCredentialsJson = "";

    public String getFcmProjectId() {
      return fcmProjectId;
    }

    public void setFcmProjectId(String fcmProjectId) {
      this.fcmProjectId = fcmProjectId;
    }

    public String getFcmCredentialsJson() {
      return fcmCredentialsJson;
    }

    public void setFcmCredentialsJson(String fcmCredentialsJson) {
      this.fcmCredentialsJson = fcmCredentialsJson;
    }

    public boolean isConfigured() {
      return fcmProjectId != null
          && !fcmProjectId.isBlank()
          && fcmCredentialsJson != null
          && !fcmCredentialsJson.isBlank();
    }
  }
}
