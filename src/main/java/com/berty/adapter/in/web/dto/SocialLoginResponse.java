package com.berty.adapter.in.web.dto;

public class SocialLoginResponse {
    private String userId;
    private String provider;
    private String accessToken;
    private boolean newUser;
    private boolean profileComplete;

    public SocialLoginResponse() {}

    public SocialLoginResponse(String userId, String provider, String accessToken, boolean newUser, boolean profileComplete) {
        this.userId = userId;
        this.provider = provider;
        this.accessToken = accessToken;
        this.newUser = newUser;
        this.profileComplete = profileComplete;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public boolean isNewUser() { return newUser; }
    public void setNewUser(boolean newUser) { this.newUser = newUser; }
    public boolean isProfileComplete() { return profileComplete; }
    public void setProfileComplete(boolean profileComplete) { this.profileComplete = profileComplete; }
}
