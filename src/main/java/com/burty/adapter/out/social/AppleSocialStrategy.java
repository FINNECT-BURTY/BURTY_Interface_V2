package com.burty.adapter.out.social;

import com.burty.config.SocialLoginProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.model.SocialProfile;
import com.burty.domain.model.SocialProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
public class AppleSocialStrategy extends AbstractOAuthCodeStrategy {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public AppleSocialStrategy(SocialLoginProperties properties, OAuthHttpClient httpClient) {
        super(properties, httpClient);
    }

    @Override
    public SocialProvider supports() {
        return SocialProvider.APPLE;
    }

    @Override
    public void customizeAuthorizeUrl(UriComponentsBuilder builder) {
        // Apple 은 첫 동의 시 사용자 정보를 POST 로 redirect URL 에 보냄.
        builder.queryParam("response_mode", "form_post");
    }

    /** Apple 은 access_token 대신 id_token (JWT) 페이로드에 sub/email 이 있음. */
    @Override
    protected String extractToken(Map<String, Object> tokenResponse) {
        Object idToken = tokenResponse.get("id_token");
        if (idToken != null) return String.valueOf(idToken);
        return super.extractToken(tokenResponse);
    }

    @Override
    public SocialProfile fetchProfile(String code, String redirectUri, String codeVerifier) {
        String idToken = exchangeAccessToken(code, redirectUri, codeVerifier);
        Map<String, Object> payload = decodeJwtPayload(idToken);
        return new SocialProfile(
                stringValue(payload.get("sub")),
                stringValue(payload.get("email")),
                null
        );
    }

    /**
     * NOTE: 운영용으로는 Apple 의 JWKS 로 서명 검증이 필요. 현재는 페이로드만 디코드.
     * BFF callback 이 Apple 의 redirect 를 받는 경로라 MITM 위험이 낮긴 하지만 보완 권장.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> decodeJwtPayload(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return OBJECT_MAPPER.readValue(payload, Map.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "Apple id_token 파싱에 실패했습니다.");
        }
    }
}
