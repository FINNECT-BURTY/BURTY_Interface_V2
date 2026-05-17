package com.burty.security;

/**
 * Auth 관련 쿠키 이름 상수. BE 가 BFF 모드에서 OAuth 콜백 직후 set 하는 쿠키들.
 * - BURTY_ACCESS: JWT access token (HttpOnly, Secure, SameSite=Lax)
 * - BURTY_REFRESH: opaque refresh token (HttpOnly, Secure, SameSite=Lax)
 *
 * 클라이언트는 별도 헤더 조작 없이 쿠키 자동 전송으로 인증된다.
 */
public final class AuthCookies {
    public static final String ACCESS = "BURTY_ACCESS";
    public static final String REFRESH = "BURTY_REFRESH";

    private AuthCookies() {}
}
