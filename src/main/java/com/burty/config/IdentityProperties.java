package com.burty.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "burty.identity")
public class IdentityProperties {

  private boolean stubMode = true;
  private Provider provider = Provider.NICE;
  private int timeoutMs = 8000;
  private final Nice nice = new Nice();
  private final Kcb kcb = new Kcb();

  public boolean isStubMode() {
    return stubMode;
  }

  public void setStubMode(boolean stubMode) {
    this.stubMode = stubMode;
  }

  public Provider getProvider() {
    return provider;
  }

  public void setProvider(Provider provider) {
    this.provider = provider;
  }

  public int getTimeoutMs() {
    return timeoutMs;
  }

  public void setTimeoutMs(int timeoutMs) {
    this.timeoutMs = timeoutMs;
  }

  public Nice getNice() {
    return nice;
  }

  public Kcb getKcb() {
    return kcb;
  }

  public enum Provider {
    NICE,
    KCB
  }

  public static class Nice {
    private String siteCode = "";
    private String sitePassword = "";
    private String verifyUrl = "https://nice.checkplus.co.kr/CheckPlusSafeModel/checkplus.cb";

    public String getSiteCode() {
      return siteCode;
    }

    public void setSiteCode(String siteCode) {
      this.siteCode = siteCode;
    }

    public String getSitePassword() {
      return sitePassword;
    }

    public void setSitePassword(String sitePassword) {
      this.sitePassword = sitePassword;
    }

    public String getVerifyUrl() {
      return verifyUrl;
    }

    public void setVerifyUrl(String verifyUrl) {
      this.verifyUrl = verifyUrl;
    }

    public boolean isConfigured() {
      return siteCode != null
          && !siteCode.isBlank()
          && sitePassword != null
          && !sitePassword.isBlank()
          && verifyUrl != null
          && !verifyUrl.isBlank();
    }
  }

  public static class Kcb {
    private String cpCode = "";
    private String licenseKey = "";
    private String verifyUrl = "https://api.kcb.co.kr/v1/identity/verify";

    public String getCpCode() {
      return cpCode;
    }

    public void setCpCode(String cpCode) {
      this.cpCode = cpCode;
    }

    public String getLicenseKey() {
      return licenseKey;
    }

    public void setLicenseKey(String licenseKey) {
      this.licenseKey = licenseKey;
    }

    public String getVerifyUrl() {
      return verifyUrl;
    }

    public void setVerifyUrl(String verifyUrl) {
      this.verifyUrl = verifyUrl;
    }

    public boolean isConfigured() {
      return cpCode != null
          && !cpCode.isBlank()
          && licenseKey != null
          && !licenseKey.isBlank()
          && verifyUrl != null
          && !verifyUrl.isBlank();
    }
  }
}
