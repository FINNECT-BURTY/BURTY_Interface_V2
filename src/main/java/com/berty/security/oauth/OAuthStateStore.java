package com.berty.security.oauth;

/**
 * OAuth {@code state}를 서버에 보관했다가 로그인 단계에서 소비합니다.
 */
public interface OAuthStateStore {

    void remember(String provider, String state);

    /**
     * @throws IllegalStateException state가 없거나 만료된 경우
     */
    void verifyAndConsume(String provider, String state);
}
