package com.burty.adapter.out.social;

import com.burty.config.SocialLoginProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.model.SocialProvider;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * Apple Sign in token endpoint 용 client_secret (ES256 JWT) 생성.
 * {@link SocialLoginProperties.Provider#getClientSecret()} 정적 값 대신 사용.
 */
@Component
public class AppleClientSecretGenerator {

    private static final String APPLE_AUDIENCE = "https://appleid.apple.com";

    public String generate(SocialLoginProperties.Provider config) {
        if (isBlank(config.getTeamId()) || isBlank(config.getKeyId()) || isBlank(config.getPrivateKey())) {
            if (!isBlank(config.getClientSecret())) {
                return config.getClientSecret();
            }
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR,
                    SocialProvider.APPLE + " client_secret(JWT) 생성에 필요한 teamId/keyId/privateKey 가 없습니다.");
        }
        try {
            PrivateKey privateKey = parsePrivateKey(config.getPrivateKey());
            Instant now = Instant.now();
            return Jwts.builder()
                    .header().keyId(config.getKeyId()).and()
                    .issuer(config.getTeamId())
                    .audience().add(APPLE_AUDIENCE).and()
                    .subject(config.getClientId())
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plusSeconds(300)))
                    .signWith(privateKey, Jwts.SIG.ES256)
                    .compact();
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR,
                    SocialProvider.APPLE + " client_secret JWT 생성에 실패했습니다.");
        }
    }

    private static PrivateKey parsePrivateKey(String pemOrBase64) throws Exception {
        String normalized = pemOrBase64
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
