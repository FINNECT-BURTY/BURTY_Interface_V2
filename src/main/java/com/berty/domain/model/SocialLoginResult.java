package com.berty.domain.model;

public class SocialLoginResult {
    private final String userId;
    private final String provider;
    private final String accessToken;
    private final boolean newUser;
    private final boolean profileComplete;

    public SocialLoginResult(String userId, String provider, String accessToken, boolean newUser, boolean profileComplete) {
        this.userId = userId;
        this.provider = provider;
        this.accessToken = accessToken;
        this.newUser = newUser;
        this.profileComplete = profileComplete;
    }

    public String getUserId() { return userId; }
    public String getProvider() { return provider; }
    public String getAccessToken() { return accessToken; }
    public boolean isNewUser() { return newUser; }
    public boolean isProfileComplete() { return profileComplete; }
}
