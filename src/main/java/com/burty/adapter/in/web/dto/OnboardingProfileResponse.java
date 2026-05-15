package com.burty.adapter.in.web.dto;

public class OnboardingProfileResponse {
    private boolean completed;
    private boolean alreadyRegistered;

    public OnboardingProfileResponse() {}

    public OnboardingProfileResponse(boolean completed, boolean alreadyRegistered) {
        this.completed = completed;
        this.alreadyRegistered = alreadyRegistered;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isAlreadyRegistered() {
        return alreadyRegistered;
    }

    public void setAlreadyRegistered(boolean alreadyRegistered) {
        this.alreadyRegistered = alreadyRegistered;
    }
}
