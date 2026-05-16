package com.burty.adapter.in.web.dto;

public class SocialLoginResponse {
    private String userId;
    private String provider;
    private String accessToken;
    private String refreshToken;
    private long accessExpiresInSeconds;
    private long refreshExpiresInSeconds;
    private boolean newUser;
    private boolean profileComplete;

    public SocialLoginResponse() {}

    public SocialLoginResponse(String userId,
                               String provider,
                               String accessToken,
                               String refreshToken,
                               long accessExpiresInSeconds,
                               long refreshExpiresInSeconds,
                               boolean newUser,
                               boolean profileComplete) {
        this.userId = userId;
        this.provider = provider;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessExpiresInSeconds = accessExpiresInSeconds;
        this.refreshExpiresInSeconds = refreshExpiresInSeconds;
        this.newUser = newUser;
        this.profileComplete = profileComplete;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public long getAccessExpiresInSeconds() { return accessExpiresInSeconds; }
    public void setAccessExpiresInSeconds(long accessExpiresInSeconds) { this.accessExpiresInSeconds = accessExpiresInSeconds; }
    public long getRefreshExpiresInSeconds() { return refreshExpiresInSeconds; }
    public void setRefreshExpiresInSeconds(long refreshExpiresInSeconds) { this.refreshExpiresInSeconds = refreshExpiresInSeconds; }
    public boolean isNewUser() { return newUser; }
    public void setNewUser(boolean newUser) { this.newUser = newUser; }
    public boolean isProfileComplete() { return profileComplete; }
    public void setProfileComplete(boolean profileComplete) { this.profileComplete = profileComplete; }
}
