package com.burty.application.service;

import com.burty.application.port.in.SocialLoginUseCase;
import com.burty.application.port.out.AuditLogPort;
import com.burty.config.SocialLoginProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.entity.SocialAccountEntity;
import com.burty.domain.entity.UserEntity;
import com.burty.domain.model.AuditEvent;
import com.burty.domain.model.SocialAuthorizeUrlResult;
import com.burty.domain.model.SocialLoginResult;
import com.burty.domain.repository.SocialAccountRepository;
import com.burty.domain.repository.UserProfileRepository;
import com.burty.domain.repository.UserRepository;
import com.burty.security.JwtTokenProvider;
import com.burty.security.oauth.OAuthStateStore;
import com.burty.util.EncryptionUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class SocialLoginService implements SocialLoginUseCase {
    private final SocialLoginProperties properties;
    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final EncryptionUtil encryptionUtil;
    private final AuditLogPort auditLogPort;
    private final OAuthStateStore oAuthStateStore;
    private final WebClient webClient;

    public SocialLoginService(SocialLoginProperties properties,
                              SocialAccountRepository socialAccountRepository,
                              UserRepository userRepository,
                              UserProfileRepository userProfileRepository,
                              JwtTokenProvider jwtTokenProvider,
                              EncryptionUtil encryptionUtil,
                              AuditLogPort auditLogPort,
                              OAuthStateStore oAuthStateStore) {
        this.properties = properties;
        this.socialAccountRepository = socialAccountRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.encryptionUtil = encryptionUtil;
        this.auditLogPort = auditLogPort;
        this.oAuthStateStore = oAuthStateStore;
        this.webClient = WebClient.create();
    }

    @Override
    public SocialAuthorizeUrlResult createAuthorizeUrl(String providerRaw, String state) {
        String provider = normalizeProvider(providerRaw);
        SocialLoginProperties.Provider config = providerConfig(provider);
        String redirectUri = config.getRedirectUri();
        String effectiveState = blank(state) ? UUID.randomUUID().toString() : state.trim();
        if (!properties.isStubMode()) {
            oAuthStateStore.remember(provider, effectiveState);
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(config.getAuthorizeUrl())
                .queryParam("response_type", "code")
                .queryParam("client_id", config.getClientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", effectiveState);
        if (!blank(config.getScope())) {
            builder.queryParam("scope", config.getScope());
        }
        if ("APPLE".equals(provider)) {
            builder.queryParam("response_mode", "form_post");
        }
        return new SocialAuthorizeUrlResult(builder.build().toUriString(), effectiveState);
    }

    @Override
    public SocialLoginResult login(String providerRaw, String code, String redirectUri, String state, String codeVerifier) {
        String provider = normalizeProvider(providerRaw);
        if (blank(code)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "소셜 로그인 authorization code가 필요합니다.");
        }
        if (!properties.isStubMode()) {
            try {
                oAuthStateStore.verifyAndConsume(provider, state);
            } catch (IllegalStateException e) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, e.getMessage());
            }
        }

        SocialProfile profile = properties.isStubMode()
                ? stubProfile(provider, code)
                : fetchProfile(provider, code, redirectUri, codeVerifier);

        String providerUserIdHash = sha256(provider + "|" + profile.providerUserId());
        SocialAccountEntity account = socialAccountRepository
                .findByProviderAndProviderUserIdHash(provider, providerUserIdHash)
                .orElse(null);

        boolean newUser = false;
        String userId;
        if (account == null) {
            UserEntity user = createSocialUser(provider, profile);
            userRepository.save(user);
            userId = user.getUserId().toString();

            account = new SocialAccountEntity();
            account.setUserId(userId);
            account.setProvider(provider);
            account.setProviderUserIdHash(providerUserIdHash);
            account.setEmailHash(blank(profile.email()) ? null : sha256(profile.email()));
            account.setEmailEncrypted(blank(profile.email()) ? null : encryptionUtil.encrypt(profile.email()));
            account.setDisplayNameEncrypted(blank(profile.displayName()) ? null : encryptionUtil.encrypt(profile.displayName()));
            socialAccountRepository.save(account);
            newUser = true;
        } else {
            userId = account.getUserId();
            account.setLastLoginAt(LocalDateTime.now());
            socialAccountRepository.save(account);
            touchUserLogin(userId);
        }

        auditLogPort.save(new AuditEvent(
                UUID.randomUUID().toString(), userId, "SOCIAL_LOGIN", provider, "SUCCESS",
                "provider=" + provider + ", newUser=" + newUser + ", state=" + safeState(state),
                LocalDateTime.now()
        ));
        boolean profileComplete = userProfileRepository.existsById(UUID.fromString(userId));
        return new SocialLoginResult(userId, provider, jwtTokenProvider.generateToken(userId), newUser, profileComplete);
    }

    private SocialProfile fetchProfile(String provider, String code, String redirectUri, String codeVerifier) {
        SocialLoginProperties.Provider config = providerConfig(provider);
        String accessToken = exchangeAccessToken(provider, config, code, redirectUri, codeVerifier);
        if ("KAKAO".equals(provider)) {
            return fetchKakaoProfile(accessToken);
        }
        if ("GOOGLE".equals(provider)) {
            return fetchGoogleProfile(accessToken);
        }
        if ("NAVER".equals(provider)) {
            return fetchNaverProfile(accessToken);
        }
        return fetchAppleProfile(accessToken);
    }

    @SuppressWarnings("unchecked")
    private String exchangeAccessToken(String provider, SocialLoginProperties.Provider config, String code, String redirectUri, String codeVerifier) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", config.getClientId());
        if (!blank(config.getClientSecret())) form.add("client_secret", config.getClientSecret());
        form.add("redirect_uri", blank(redirectUri) ? config.getRedirectUri() : redirectUri);
        form.add("code", code);
        if (!blank(codeVerifier)) {
            form.add("code_verifier", codeVerifier);
        }

        Map<String, Object> response = webClient.post()
                .uri(config.getTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(Map.class)
                .block(Duration.ofMillis(properties.getTimeoutMs()));

        if (response == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, provider + " 토큰 응답이 비어 있습니다.");
        }
        Object token = response.get("access_token");
        if ("APPLE".equals(provider) && response.get("id_token") != null) {
            token = response.get("id_token");
        }
        if (token == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, provider + " 토큰 발급에 실패했습니다.");
        }
        return String.valueOf(token);
    }

    @SuppressWarnings("unchecked")
    private SocialProfile fetchKakaoProfile(String accessToken) {
        Map<String, Object> response = getUserInfo("KAKAO", accessToken);
        String id = String.valueOf(response.get("id"));
        Map<String, Object> account = response.get("kakao_account") instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
        Map<String, Object> profile = account.get("profile") instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
        return new SocialProfile(id, stringValue(account.get("email")), stringValue(profile.get("nickname")));
    }

    @SuppressWarnings("unchecked")
    private SocialProfile fetchNaverProfile(String accessToken) {
        Map<String, Object> response = getUserInfo("NAVER", accessToken);
        Map<String, Object> body = response.get("response") instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
        return new SocialProfile(stringValue(body.get("id")), stringValue(body.get("email")), stringValue(body.get("name")));
    }

    private SocialProfile fetchGoogleProfile(String accessToken) {
        Map<String, Object> response = getUserInfo("GOOGLE", accessToken);
        return new SocialProfile(
                stringValue(response.get("sub")),
                stringValue(response.get("email")),
                stringValue(response.get("name"))
        );
    }

    private SocialProfile fetchAppleProfile(String idToken) {
        Map<String, Object> payload = decodeJwtPayload(idToken);
        return new SocialProfile(stringValue(payload.get("sub")), stringValue(payload.get("email")), null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getUserInfo(String provider, String accessToken) {
        Map<String, Object> response = webClient.get()
                .uri(providerConfig(provider).getUserInfoUrl())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(Map.class)
                .block(Duration.ofMillis(properties.getTimeoutMs()));
        if (response == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, provider + " 사용자 정보 응답이 비어 있습니다.");
        }
        return response;
    }

    private UserEntity createSocialUser(String provider, SocialProfile profile) {
        LocalDateTime now = LocalDateTime.now();
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setUserId(userId);
        user.setCiHash(sha256("SOCIAL_CI|" + provider + "|" + profile.providerUserId()));
        user.setCiEncrypted(encryptionUtil.encrypt("SOCIAL_CI|" + provider + "|" + profile.providerUserId()).getBytes(StandardCharsets.UTF_8));
        user.setPhoneHash(sha256("SOCIAL_PHONE|" + provider + "|" + profile.providerUserId()));
        user.setPhoneEncrypted(encryptionUtil.encrypt("SOCIAL_PHONE_UNVERIFIED").getBytes(StandardCharsets.UTF_8));
        user.setStatus(UserEntity.UserStatus.ACTIVE);
        user.setFailedLoginCount(0);
        user.setLastLoginAt(now);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return user;
    }

    private void touchUserLogin(String userId) {
        try {
            userRepository.findById(UUID.fromString(userId)).ifPresent(user -> {
                user.setLastLoginAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
            });
        } catch (IllegalArgumentException ignored) {
        }
    }

    private SocialProfile stubProfile(String provider, String code) {
        String id = provider.toLowerCase() + "-" + sha256(code).substring(0, 16);
        return new SocialProfile(id, id + "@stub.burty.local", provider + " 사용자");
    }

    private SocialLoginProperties.Provider providerConfig(String provider) {
        return switch (provider) {
            case "KAKAO" -> properties.getKakao();
            case "GOOGLE" -> properties.getGoogle();
            case "NAVER" -> properties.getNaver();
            case "APPLE" -> properties.getApple();
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 소셜 로그인 제공자입니다.");
        };
    }

    private String normalizeProvider(String provider) {
        if (provider == null) throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "provider가 필요합니다.");
        String normalized = provider.trim().toUpperCase();
        if (!normalized.equals("GOOGLE") && !normalized.equals("KAKAO") && !normalized.equals("NAVER") && !normalized.equals("APPLE")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "provider는 GOOGLE, KAKAO, NAVER, APPLE 중 하나여야 합니다.");
        }
        return normalized;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> decodeJwtPayload(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(payload, Map.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "Apple id_token 파싱에 실패했습니다.");
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 hash failed", e);
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String safeState(String state) {
        if (state == null) return "";
        return state.length() <= 80 ? state : state.substring(0, 80);
    }

    private record SocialProfile(String providerUserId, String email, String displayName) {}
}
