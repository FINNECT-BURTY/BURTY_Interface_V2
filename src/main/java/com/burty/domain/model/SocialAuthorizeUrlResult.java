package com.burty.domain.model;

/**
 * OAuth 인가 URL과 CSRF 방지용 state(서버가 발급하거나 클라이언트가 전달한 값).
 */
public record SocialAuthorizeUrlResult(String authorizeUrl, String state) {}
