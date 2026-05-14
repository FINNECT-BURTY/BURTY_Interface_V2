package com.berty.adapter.in.web.dto;

public class SocialLoginRequest {
    private String code;
    private String redirectUri;
    private String state;
    /** PKCE: 토큰 엔드포인트에 전달할 {@code code_verifier} (선택). */
    private String codeVerifier;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCodeVerifier() { return codeVerifier; }
    public void setCodeVerifier(String codeVerifier) { this.codeVerifier = codeVerifier; }
}
