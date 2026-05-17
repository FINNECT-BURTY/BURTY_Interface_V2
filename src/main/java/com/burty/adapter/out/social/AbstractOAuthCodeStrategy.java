package com.burty.adapter.out.social;

import com.burty.config.SocialLoginProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.model.SocialProvider;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

/**
 * 표준 OAuth 2.0 Authorization Code flow 공통부.
 * - token endpoint POST form 구성
 * - access_token 추출 (Apple 만 id_token 우선이므로 override 지점 제공)
 *
 * KAKAO/GOOGLE/NAVER 는 이 추상 클래스 + userinfo 파싱만 구현하면 됨.
 * APPLE 은 userinfo endpoint 가 없고 id_token 자체를 파싱하므로 fetchProfile 을 통째로 override.
 */
public abstract class AbstractOAuthCodeStrategy implements SocialProviderStrategy {
    protected final SocialLoginProperties properties;
    protected final OAuthHttpClient httpClient;

    protected AbstractOAuthCodeStrategy(SocialLoginProperties properties, OAuthHttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    protected SocialLoginProperties.Provider config() {
        return properties.get(supports());
    }

    /** authorization_code 로 access_token 교환. Apple 의 id_token 케이스는 extractToken override. */
    protected String exchangeAccessToken(String code, String redirectUri, String codeVerifier) {
        SocialLoginProperties.Provider cfg = config();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", cfg.getClientId());
        if (notBlank(cfg.getClientSecret())) form.add("client_secret", cfg.getClientSecret());
        form.add("redirect_uri", notBlank(redirectUri) ? redirectUri : cfg.getRedirectUri());
        form.add("code", code);
        if (notBlank(codeVerifier)) form.add("code_verifier", codeVerifier);

        Map<String, Object> response = httpClient.postForm(supports(), cfg.getTokenUrl(), form, "token");
        return extractToken(response);
    }

    /** 기본 access_token. Apple 은 id_token 우선이라 override. */
    protected String extractToken(Map<String, Object> tokenResponse) {
        Object token = tokenResponse.get("access_token");
        if (token == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, supports() + " 토큰 발급에 실패했습니다.");
        }
        return String.valueOf(token);
    }

    protected Map<String, Object> userInfo(String bearerToken) {
        return httpClient.getJson(supports(), config().getUserInfoUrl(), bearerToken, "userinfo");
    }

    protected static String stringValue(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    @SuppressWarnings("unchecked")
    protected static Map<String, Object> mapOrEmpty(Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    protected static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
