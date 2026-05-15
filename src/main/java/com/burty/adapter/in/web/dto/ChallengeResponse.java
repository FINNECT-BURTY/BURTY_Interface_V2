package com.burty.adapter.in.web.dto;

public class ChallengeResponse {
    private String challengeId;

    public ChallengeResponse() {}

    public ChallengeResponse(String challengeId) {
        this.challengeId = challengeId;
    }

    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
}
