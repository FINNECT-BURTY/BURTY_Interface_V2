package com.burty.application.service;

import com.burty.adapter.out.social.SocialProviderRegistry;
import com.burty.adapter.out.social.SocialProviderStrategy;
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
import com.burty.domain.model.SocialProfile;
import com.burty.domain.model.SocialProvider;
import com.burty.domain.repository.SocialAccountRepository;
import com.burty.domain.repository.UserProfileRepository;
import com.burty.domain.repository.UserRepository;
import com.burty.security.RefreshTokenService;
import com.burty.security.oauth.OAuthStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SocialLoginService implements SocialLoginUseCase {
    private static final Logger log = LoggerFactory.getLogger(SocialLoginService.class);

    private final SocialLoginProperties properties;
    private final SocialProviderRegistry providerRegistry;
    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogPort auditLogPort;
    private final OAuthStateStore oAuthStateStore;

    public SocialLoginService(SocialLoginProperties properties,
                              SocialProviderRegistry providerRegistry,
                              SocialAccountRepository socialAccountRepository,
                              UserRepository userRepository,
                              UserProfileRepository userProfileRepository,
                              RefreshTokenService refreshTokenService,
                              AuditLogPort auditLogPort,
                              OAuthStateStore oAuthStateStore) {
        this.properties = properties;
        this.providerRegistry = providerRegistry;
        this.socialAccountRepository = socialAccountRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.refreshTokenService = refreshTokenService;
        this.auditLogPort = auditLogPort;
        this.oAuthStateStore = oAuthStateStore;
    }

    @Override
    public SocialAuthorizeUrlResult createAuthorizeUrl(String providerRaw, String state, String requestedRedirectUri) {
        SocialProvider provider = SocialProvider.parse(providerRaw);
        SocialProviderStrategy strategy = providerRegistry.get(provider);
        SocialLoginProperties.Provider config = properties.get(provider);

        // 호출자가 redirect_uri 를 지정했으면 그 값을 사용. 단 provider 콘솔에 등록된 URI 와 정확히 일치해야 함
        // (등록 안 된 값이면 카카오 KOE006 / 구글 redirect_uri_mismatch 등).
        String redirectUri = blank(requestedRedirectUri) ? config.getRedirectUri() : requestedRedirectUri.trim();
        String effectiveState = blank(state) ? UUID.randomUUID().toString() : state.trim();
        if (!properties.isStubMode()) {
            oAuthStateStore.remember(provider.name(), effectiveState);
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(config.getAuthorizeUrl())
                .queryParam("response_type", "code")
                .queryParam("client_id", config.getClientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", effectiveState);
        if (!blank(config.getScope())) builder.queryParam("scope", config.getScope());
        strategy.customizeAuthorizeUrl(builder);

        return new SocialAuthorizeUrlResult(builder.build().toUriString(), effectiveState);
    }

    @Override
    @Transactional
    public SocialLoginResult login(String providerRaw, String code, String redirectUri, String state, String codeVerifier) {
        SocialProvider provider = SocialProvider.parse(providerRaw);
        log.info("Social login start provider={} stubMode={} hasState={} hasRedirectUri={} hasCodeVerifier={}",
                provider, properties.isStubMode(), !blank(state), !blank(redirectUri), !blank(codeVerifier));

        if (blank(code)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "소셜 로그인 authorization code가 필요합니다.");
        }
        verifyState(provider, state);
        log.debug("Social login state verified provider={}", provider);

        SocialProfile profile = properties.isStubMode()
                ? stubProfile(provider, code)
                : providerRegistry.get(provider).fetchProfile(code, redirectUri, codeVerifier);
        if (blank(profile.providerUserId())) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, provider + " 사용자 식별자를 확인할 수 없습니다.");
        }
        log.info("Social login profile fetched provider={} hasEmail={} hasDisplayName={}",
                provider, !blank(profile.email()), !blank(profile.displayName()));

        String providerUserIdHash = sha256(provider.name() + "|" + profile.providerUserId());
        SocialAccountEntity account = socialAccountRepository
                .findByProviderAndProviderUserIdHash(provider.name(), providerUserIdHash)
                .orElse(null);

        boolean newUser;
        Long userId;
        if (account == null) {
            newUser = true;
            userId = createNewSocialUser(provider, profile, providerUserIdHash);
        } else {
            Long existingUserId = account.getUserId();
            UserEntity user = userRepository.findById(existingUserId).orElse(null);
            if (user == null) {
                log.warn("Social account points to missing user. Recreating account provider={} userId={} socialAccountId={}",
                        provider, existingUserId, account.getSocialAccountId());
                socialAccountRepository.delete(account);
                socialAccountRepository.flush();
                newUser = true;
                userId = createNewSocialUser(provider, profile, providerUserIdHash);
            } else {
                newUser = false;
                userId = touchExistingAccount(account, user);
            }
        }
        log.info("Social login user resolved provider={} userId={} newUser={}", provider, userId, newUser);

        boolean profileComplete = userProfileRepository.existsById(userId);
        RefreshTokenService.TokenPair tokens = refreshTokenService.issueNewSession(userId.toString(), null);
        safeAudit(userId.toString(), provider, newUser, state);
        log.info("Social login complete provider={} userId={} newUser={} profileComplete={}",
                provider, userId, newUser, profileComplete);

        return new SocialLoginResult(
                userId.toString(),
                provider.name(),
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.accessExpiresInSeconds(),
                tokens.refreshExpiresInSeconds(),
                newUser,
                profileComplete
        );
    }

    private void verifyState(SocialProvider provider, String state) {
        if (properties.isStubMode()) return;
        try {
            oAuthStateStore.verifyAndConsume(provider.name(), state);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, e.getMessage());
        }
    }

    private Long createNewSocialUser(SocialProvider provider, SocialProfile profile, String providerUserIdHash) {
        UserEntity user = userRepository.save(buildSocialUser(provider, profile));
        Long userId = user.getUserId();

        SocialAccountEntity account = new SocialAccountEntity();
        account.setUserId(userId);
        account.setProvider(provider.name());
        account.setProviderUserIdHash(providerUserIdHash);
        account.setEmailHash(blank(profile.email()) ? null : sha256(profile.email()));
        account.setEmail(blank(profile.email()) ? null : profile.email());
        account.setDisplayName(blank(profile.displayName()) ? null : profile.displayName());
        socialAccountRepository.save(account);
        return userId;
    }

    private Long touchExistingAccount(SocialAccountEntity account, UserEntity user) {
        Long userId = account.getUserId();
        account.setLastLoginAt(LocalDateTime.now());
        socialAccountRepository.save(account);
        LocalDateTime now = LocalDateTime.now();
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);
        return userId;
    }

    private UserEntity buildSocialUser(SocialProvider provider, SocialProfile profile) {
        LocalDateTime now = LocalDateTime.now();
        String seed = provider.name() + "|" + profile.providerUserId();
        UserEntity user = new UserEntity();
        user.setCiHash(sha256("SOCIAL_CI|" + seed));
        user.setCi("SOCIAL_CI|" + seed);
        user.setPhoneHash(sha256("SOCIAL_PHONE|" + seed));
        user.setPhone("SOCIAL_PHONE_UNVERIFIED");
        user.setStatus(UserEntity.UserStatus.ACTIVE);
        user.setFailedLoginCount(0);
        user.setLastLoginAt(now);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return user;
    }

    private SocialProfile stubProfile(SocialProvider provider, String code) {
        String id = provider.name().toLowerCase() + "-" + sha256(code).substring(0, 16);
        return new SocialProfile(id, id + "@stub.burty.local", provider.name() + " 사용자");
    }

    private void safeAudit(String userId, SocialProvider provider, boolean newUser, String state) {
        try {
            auditLogPort.save(new AuditEvent(
                    UUID.randomUUID().toString(), userId, "SOCIAL_LOGIN", provider.name(), "SUCCESS",
                    "provider=" + provider + ", newUser=" + newUser + ", state=" + truncateForAudit(state),
                    LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.warn("social login audit save failed userId={} provider={} reason={}",
                    userId, provider, e.getClass().getSimpleName());
        }
    }

    private static String sha256(String value) {
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

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String truncateForAudit(String state) {
        if (state == null) return "";
        return state.length() <= 80 ? state : state.substring(0, 80);
    }
}
