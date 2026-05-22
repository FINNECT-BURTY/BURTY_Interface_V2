package com.burty.security.oauth;

/**
 * {@link OAuthStateStore#verifyAndConsume} 성공 시 authorize 단계에서 저장한 컨텍스트.
 */
public record OAuthStateContext(String frontendOrigin) {
}
