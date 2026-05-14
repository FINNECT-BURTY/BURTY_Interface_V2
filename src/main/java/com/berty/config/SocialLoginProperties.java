package com.berty.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "berty.social")
public class SocialLoginProperties {
    private boolean stubMode = true;
    private long timeoutMs = 5000;
    private Provider kakao = new Provider();
    private Provider google = new Provider();
    private Provider naver = new Provider();
    private Provider apple = new Provider();

    public boolean isStubMode() { return stubMode; }
    public void setStubMode(boolean stubMode) { this.stubMode = stubMode; }
    public long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
    public Provider getKakao() { return kakao; }
    public void setKakao(Provider kakao) { this.kakao = kakao; }
    public Provider getGoogle() { return google; }
    public void setGoogle(Provider google) { this.google = google; }
    public Provider getNaver() { return naver; }
    public void setNaver(Provider naver) { this.naver = naver; }
    public Provider getApple() { return apple; }
    public void setApple(Provider apple) { this.apple = apple; }

    public static class Provider {
        private String clientId = "";
        private String clientSecret = "";
        private String redirectUri = "";
        private String authorizeUrl = "";
        private String tokenUrl = "";
        private String userInfoUrl = "";
        private String scope = "";
        private String teamId = "";
        private String keyId = "";
        private String privateKey = "";

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
        public String getAuthorizeUrl() { return authorizeUrl; }
        public void setAuthorizeUrl(String authorizeUrl) { this.authorizeUrl = authorizeUrl; }
        public String getTokenUrl() { return tokenUrl; }
        public void setTokenUrl(String tokenUrl) { this.tokenUrl = tokenUrl; }
        public String getUserInfoUrl() { return userInfoUrl; }
        public void setUserInfoUrl(String userInfoUrl) { this.userInfoUrl = userInfoUrl; }
        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }
        public String getTeamId() { return teamId; }
        public void setTeamId(String teamId) { this.teamId = teamId; }
        public String getKeyId() { return keyId; }
        public void setKeyId(String keyId) { this.keyId = keyId; }
        public String getPrivateKey() { return privateKey; }
        public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
    }
}
