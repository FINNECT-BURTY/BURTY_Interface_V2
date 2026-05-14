package com.berty.adapter.in.web.dto;

public class AuthorizeUrlResponse {
    private String authorizeUrl;
    /** OAuth CSRF 방지용 state(비어 있으면 서버가 생성해 URL에 포함). */
    private String state;

    public AuthorizeUrlResponse() {}

    public AuthorizeUrlResponse(String authorizeUrl) {
        this(authorizeUrl, null);
    }

    public AuthorizeUrlResponse(String authorizeUrl, String state) {
        this.authorizeUrl = authorizeUrl;
        this.state = state;
    }

    public String getAuthorizeUrl() { return authorizeUrl; }
    public void setAuthorizeUrl(String authorizeUrl) { this.authorizeUrl = authorizeUrl; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}
