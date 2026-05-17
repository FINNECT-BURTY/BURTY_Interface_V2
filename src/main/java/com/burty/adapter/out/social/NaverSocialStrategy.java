package com.burty.adapter.out.social;

import com.burty.config.SocialLoginProperties;
import com.burty.domain.model.SocialProfile;
import com.burty.domain.model.SocialProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NaverSocialStrategy extends AbstractOAuthCodeStrategy {

    public NaverSocialStrategy(SocialLoginProperties properties, OAuthHttpClient httpClient) {
        super(properties, httpClient);
    }

    @Override
    public SocialProvider supports() {
        return SocialProvider.NAVER;
    }

    @Override
    public SocialProfile fetchProfile(String code, String redirectUri, String codeVerifier) {
        String accessToken = exchangeAccessToken(code, redirectUri, codeVerifier);
        Map<String, Object> response = userInfo(accessToken);
        Map<String, Object> body = mapOrEmpty(response.get("response"));
        return new SocialProfile(
                stringValue(body.get("id")),
                stringValue(body.get("email")),
                stringValue(body.get("name"))
        );
    }
}
