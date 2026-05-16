package com.burty.adapter.in.web.dto;

public class TokenPairResponse {
    private String accessToken;
    private String refreshToken;
    private long accessExpiresInSeconds;
    private long refreshExpiresInSeconds;

    public TokenPairResponse() {}

    public TokenPairResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, 0L, 0L);
    }

    public TokenPairResponse(String accessToken,
                             String refreshToken,
                             long accessExpiresInSeconds,
                             long refreshExpiresInSeconds) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessExpiresInSeconds = accessExpiresInSeconds;
        this.refreshExpiresInSeconds = refreshExpiresInSeconds;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public long getAccessExpiresInSeconds() { return accessExpiresInSeconds; }
    public void setAccessExpiresInSeconds(long accessExpiresInSeconds) { this.accessExpiresInSeconds = accessExpiresInSeconds; }
    public long getRefreshExpiresInSeconds() { return refreshExpiresInSeconds; }
    public void setRefreshExpiresInSeconds(long refreshExpiresInSeconds) { this.refreshExpiresInSeconds = refreshExpiresInSeconds; }
}
