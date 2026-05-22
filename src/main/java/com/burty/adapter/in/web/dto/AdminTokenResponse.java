package com.burty.adapter.in.web.dto;

public class AdminTokenResponse {
    private String accessToken;

    public AdminTokenResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() { return accessToken; }
}
