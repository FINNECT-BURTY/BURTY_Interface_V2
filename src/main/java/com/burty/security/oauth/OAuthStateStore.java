package com.burty.security.oauth;

/**
 * OAuth {@code state}를 서버에 보관했다가 로그인 단계에서 소비합니다.
 */
public interface OAuthStateStore {

    /**
     * @param frontendOrigin 로그인 완료 후 돌려보낼 FE origin (nullable)
     */
    void remember(String provider, String state, String frontendOrigin);

    /**
     * @throws IllegalStateException state가 없거나 만료된 경우
     */
    OAuthStateContext verifyAndConsume(String provider, String state);
}
