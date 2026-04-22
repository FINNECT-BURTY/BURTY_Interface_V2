package com.nuri.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nuri.mydata")
public class MyDataProperties {
    private String clientId = "nuri-client";
    private String clientSecret = "nuri-secret";
    private String authorizeUrl = "https://sandbox.mydata.local/oauth2/authorize";
    private String tokenUrl = "https://sandbox.mydata.local/oauth2/token";
    private String refreshUrl = "https://sandbox.mydata.local/oauth2/token";
    private String assetUrl = "https://sandbox.mydata.local/api/v1/assets";
    private String redirectUri = "http://localhost:8080/api/nuri/mydata/oauth/callback";
    private String scope = "asset.read transfer.read";
    private boolean stubMode = true;
    private int retryCount = 2;

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public String getAuthorizeUrl() { return authorizeUrl; }
    public void setAuthorizeUrl(String authorizeUrl) { this.authorizeUrl = authorizeUrl; }
    public String getTokenUrl() { return tokenUrl; }
    public void setTokenUrl(String tokenUrl) { this.tokenUrl = tokenUrl; }
    public String getAssetUrl() { return assetUrl; }
    public void setAssetUrl(String assetUrl) { this.assetUrl = assetUrl; }
    public String getRefreshUrl() { return refreshUrl; }
    public void setRefreshUrl(String refreshUrl) { this.refreshUrl = refreshUrl; }
    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public boolean isStubMode() { return stubMode; }
    public void setStubMode(boolean stubMode) { this.stubMode = stubMode; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
}
