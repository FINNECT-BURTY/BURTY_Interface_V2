package com.burty.adapter.out.social;

import com.burty.config.SocialLoginProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.model.SocialProfile;
import com.burty.domain.model.SocialProvider;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class AppleSocialStrategy extends AbstractOAuthCodeStrategy {
    private final AppleClientSecretGenerator clientSecretGenerator;
    private final AppleIdTokenVerifier idTokenVerifier;

    public AppleSocialStrategy(SocialLoginProperties properties,
                               OAuthHttpClient httpClient,
                               AppleClientSecretGenerator clientSecretGenerator,
                               AppleIdTokenVerifier idTokenVerifier) {
        super(properties, httpClient);
        this.clientSecretGenerator = clientSecretGenerator;
        this.idTokenVerifier = idTokenVerifier;
    }

    @Override
    public SocialProvider supports() {
        return SocialProvider.APPLE;
    }

    @Override
    protected String resolveClientSecret(SocialLoginProperties.Provider cfg) {
        return clientSecretGenerator.generate(cfg);
    }

    @Override
    public void customizeAuthorizeUrl(UriComponentsBuilder builder) {
        builder.queryParam("response_mode", "form_post");
    }

    @Override
    protected String extractToken(Map<String, Object> tokenResponse) {
        Object idToken = tokenResponse.get("id_token");
        if (idToken != null) return String.valueOf(idToken);
        return super.extractToken(tokenResponse);
    }

    @Override
    public SocialProfile fetchProfile(String code, String redirectUri, String codeVerifier) {
        String idToken = exchangeAccessToken(code, redirectUri, codeVerifier);
        Claims claims = idTokenVerifier.verify(idToken, config().getClientId());
        return new SocialProfile(
                claims.getSubject(),
                claims.get("email", String.class),
                null
        );
    }
}
