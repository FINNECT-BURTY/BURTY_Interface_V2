package com.burty.adapter.out.social;

import com.burty.config.SocialLoginProperties;
import com.burty.domain.model.SocialProfile;
import com.burty.domain.model.SocialProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class KakaoSocialStrategy extends AbstractOAuthCodeStrategy {

    public KakaoSocialStrategy(SocialLoginProperties properties, OAuthHttpClient httpClient) {
        super(properties, httpClient);
    }

    @Override
    public SocialProvider supports() {
        return SocialProvider.KAKAO;
    }

    @Override
    public SocialProfile fetchProfile(String code, String redirectUri, String codeVerifier) {
        String accessToken = exchangeAccessToken(code, redirectUri, codeVerifier);
        Map<String, Object> response = userInfo(accessToken);
        Map<String, Object> account = mapOrEmpty(response.get("kakao_account"));
        Map<String, Object> profile = mapOrEmpty(account.get("profile"));

        String id = stringValue(response.get("id"));
        String email = extractEmail(account);
        String nickname = extractNickname(account, profile);

        return new SocialProfile(id, email, nickname);
    }

    /**
     * 이메일은 동의 + 검증 통과한 경우에만 사용. 검수 안 된 앱은 플래그 없이 email 만 오는 케이스가 있어
     * 그 경우도 fallback 으로 허용. 동의 안 했으면 null 로 두고 후속 가입 흐름이 이어지게 함.
     */
    private String extractEmail(Map<String, Object> account) {
        boolean emailUsable = Boolean.TRUE.equals(account.get("has_email"))
                && Boolean.TRUE.equals(account.get("is_email_valid"))
                && Boolean.TRUE.equals(account.get("is_email_verified"))
                && !Boolean.TRUE.equals(account.get("email_needs_agreement"));
        if (emailUsable) return stringValue(account.get("email"));
        if (account.containsKey("email")) return stringValue(account.get("email"));
        return null;
    }

    private String extractNickname(Map<String, Object> account, Map<String, Object> profile) {
        if (Boolean.TRUE.equals(account.get("profile_nickname_needs_agreement"))) return null;
        return stringValue(profile.get("nickname"));
    }
}
