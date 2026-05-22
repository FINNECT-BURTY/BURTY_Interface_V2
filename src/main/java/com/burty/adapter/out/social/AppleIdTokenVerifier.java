package com.burty.adapter.out.social;

import com.burty.config.SocialLoginProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.model.SocialProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Apple id_token JWKS 서명 검증.
 */
@Component
public class AppleIdTokenVerifier {

    private static final String JWKS_URL = "https://appleid.apple.com/auth/keys";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final Duration timeout;
    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();
    private volatile JsonNode jwksCache;
    private volatile long jwksFetchedAtMs;

    public AppleIdTokenVerifier(SocialLoginProperties properties) {
        this.webClient = WebClient.create();
        this.timeout = Duration.ofMillis(properties.getTimeoutMs());
    }

    public Claims verify(String idToken, String expectedClientId) {
        try {
            String kid = extractKid(idToken);
            PublicKey publicKey = resolvePublicKey(kid);
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer("https://appleid.apple.com")
                    .requireAudience(expectedClientId)
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "Apple id_token 서명 검증에 실패했습니다.");
        }
    }

    private String extractKid(String jwt) {
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "Apple id_token 형식이 올바르지 않습니다.");
        }
        try {
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String kid = MAPPER.readTree(headerJson).path("kid").asText(null);
            if (kid == null || kid.isBlank()) {
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "Apple id_token kid 가 없습니다.");
            }
            return kid;
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "Apple id_token 헤더 파싱에 실패했습니다.");
        }
    }

    private PublicKey resolvePublicKey(String kid) throws Exception {
        PublicKey cached = keyCache.get(kid);
        if (cached != null) {
            return cached;
        }
        refreshJwksIfStale();
        PublicKey fromCache = findKey(kid);
        if (fromCache != null) {
            keyCache.put(kid, fromCache);
            return fromCache;
        }
        refreshJwksForce();
        PublicKey refreshed = findKey(kid);
        if (refreshed == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "Apple JWKS 에서 kid=" + kid + " 키를 찾을 수 없습니다.");
        }
        keyCache.put(kid, refreshed);
        return refreshed;
    }

    private PublicKey findKey(String kid) throws Exception {
        for (JsonNode key : jwksCache.path("keys")) {
            if (kid.equals(key.path("kid").asText())) {
                return toRsaPublicKey(key);
            }
        }
        return null;
    }

    private void refreshJwksIfStale() {
        if (jwksCache != null && System.currentTimeMillis() - jwksFetchedAtMs < Duration.ofHours(6).toMillis()) {
            return;
        }
        refreshJwksForce();
    }

    private void refreshJwksForce() {
        String body = webClient.get()
                .uri(JWKS_URL)
                .retrieve()
                .bodyToMono(String.class)
                .block(timeout);
        try {
            jwksCache = MAPPER.readTree(body);
            jwksFetchedAtMs = System.currentTimeMillis();
            keyCache.clear();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR,
                    SocialProvider.APPLE + " JWKS 조회에 실패했습니다.");
        }
    }

    private static PublicKey toRsaPublicKey(JsonNode key) throws Exception {
        byte[] nBytes = Base64.getUrlDecoder().decode(key.path("n").asText());
        byte[] eBytes = Base64.getUrlDecoder().decode(key.path("e").asText());
        BigInteger modulus = new BigInteger(1, nBytes);
        BigInteger exponent = new BigInteger(1, eBytes);
        return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
    }
}
