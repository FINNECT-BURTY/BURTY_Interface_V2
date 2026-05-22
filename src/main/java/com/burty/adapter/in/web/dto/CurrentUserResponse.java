package com.burty.adapter.in.web.dto;

public class CurrentUserResponse {
    private String userId;
    private boolean profileComplete;

    public CurrentUserResponse() {}

    public CurrentUserResponse(String userId, boolean profileComplete) {
        this.userId = userId;
        this.profileComplete = profileComplete;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isProfileComplete() {
        return profileComplete;
    }

    public void setProfileComplete(boolean profileComplete) {
        this.profileComplete = profileComplete;
    }
}
