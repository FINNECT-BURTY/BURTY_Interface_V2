package com.burty.domain.model;

public class SocialLoginResult {
    private final String userId;
    private final String provider;
    private final String accessToken;
    private final String refreshToken;
    private final long accessExpiresInSeconds;
    private final long refreshExpiresInSeconds;
    private final boolean newUser;
    private final boolean profileComplete;
    private final String frontendOrigin;

    public SocialLoginResult(String userId,
                             String provider,
                             String accessToken,
                             String refreshToken,
                             long accessExpiresInSeconds,
                             long refreshExpiresInSeconds,
                             boolean newUser,
                             boolean profileComplete,
                             String frontendOrigin) {
        this.userId = userId;
        this.provider = provider;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessExpiresInSeconds = accessExpiresInSeconds;
        this.refreshExpiresInSeconds = refreshExpiresInSeconds;
        this.newUser = newUser;
        this.profileComplete = profileComplete;
        this.frontendOrigin = frontendOrigin;
    }

    public String getUserId() { return userId; }
    public String getProvider() { return provider; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public long getAccessExpiresInSeconds() { return accessExpiresInSeconds; }
    public long getRefreshExpiresInSeconds() { return refreshExpiresInSeconds; }
    public boolean isNewUser() { return newUser; }
    public boolean isProfileComplete() { return profileComplete; }
    public String getFrontendOrigin() { return frontendOrigin; }
}
