package com.burty.adapter.out.social;

import com.burty.config.SocialLoginProperties;
import com.burty.domain.model.SocialProfile;
import com.burty.domain.model.SocialProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GoogleSocialStrategy extends AbstractOAuthCodeStrategy {

    public GoogleSocialStrategy(SocialLoginProperties properties, OAuthHttpClient httpClient) {
        super(properties, httpClient);
    }

    @Override
    public SocialProvider supports() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public SocialProfile fetchProfile(String code, String redirectUri, String codeVerifier) {
        String accessToken = exchangeAccessToken(code, redirectUri, codeVerifier);
        Map<String, Object> response = userInfo(accessToken);
        return new SocialProfile(
                stringValue(response.get("sub")),
                stringValue(response.get("email")),
                stringValue(response.get("name"))
        );
    }
}
