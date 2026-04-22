package com.nuri.adapter.in.web.dto;

public class WebAuthnFinishRequest {
    private String userId;
    private String challengeId;
    private String payload;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}
