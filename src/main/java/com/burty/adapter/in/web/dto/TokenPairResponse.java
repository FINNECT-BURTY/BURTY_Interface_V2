package com.burty.adapter.in.web.dto;

public class TokenPairResponse {
    private String accessToken;
    private String refreshToken;

    public TokenPairResponse() {}

    public TokenPairResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
