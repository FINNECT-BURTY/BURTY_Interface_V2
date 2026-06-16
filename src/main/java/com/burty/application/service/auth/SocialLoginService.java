/**
 *
 *
 * <pre>
 * <b>Description  : 인증 애플리케이션 서비스 (SocialLoginService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.auth
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.application.service.auth;

import com.burty.adapter.out.social.SocialProviderRegistry;
import com.burty.adapter.out.social.SocialProviderStrategy;
import com.burty.application.port.in.auth.SocialLoginUseCase;
import com.burty.application.service.support.AuditLogger;
import com.burty.config.SocialLoginProperties;
import com.burty.core.constant.LogMessages;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.auth.entity.SocialAccountEntity;
import com.burty.domain.auth.model.SocialAuthorizeUrlResult;
import com.burty.domain.auth.model.SocialLoginResult;
import com.burty.domain.auth.model.SocialProfile;
import com.burty.domain.auth.model.SocialProvider;
import com.burty.domain.auth.repository.SocialAccountRepository;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.UserProfileRepository;
import com.burty.domain.user.repository.UserRepository;
import com.burty.security.RefreshTokenService;
import com.burty.security.oauth.OAuthStateContext;
import com.burty.security.oauth.OAuthStateStore;
import com.burty.util.AccountNumberHasher;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class SocialLoginService implements SocialLoginUseCase {

  private static final Logger log = LoggerFactory.getLogger(SocialLoginService.class);

  private final SocialLoginProperties properties;
  private final SocialProviderRegistry providerRegistry;
  private final SocialAccountRepository socialAccountRepository;
  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;
  private final RefreshTokenService refreshTokenService;
  private final AuditLogger auditLogger;
  private final OAuthStateStore oAuthStateStore;
  private final AccountNumberHasher hasher;

  @Override
  public SocialAuthorizeUrlResult createAuthorizeUrl(
      String providerRaw, String state, String frontendOrigin) {
    SocialProvider provider = SocialProvider.parse(providerRaw);
    SocialProviderStrategy strategy = providerRegistry.get(provider);
    SocialLoginProperties.Provider config = properties.get(provider);

    String effectiveState = blank(state) ? UUID.randomUUID().toString() : state.trim();
    if (!properties.isStubMode()) {
      oAuthStateStore.remember(provider.name(), effectiveState, frontendOrigin);
    }

    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(config.getAuthorizeUrl())
            .queryParam("response_type", "code")
            .queryParam("client_id", config.getClientId())
            .queryParam("redirect_uri", config.getRedirectUri())
            .queryParam("state", effectiveState);
    if (!blank(config.getScope())) {
      builder.queryParam("scope", config.getScope());
    }
    strategy.customizeAuthorizeUrl(builder);

    return new SocialAuthorizeUrlResult(builder.build().toUriString(), effectiveState);
  }

  @Override
  @Transactional
  public SocialLoginResult login(
      String providerRaw, String code, String redirectUri, String state, String codeVerifier) {
    SocialProvider provider = SocialProvider.parse(providerRaw);
    log.info(
        LogMessages.Auth.SOCIAL_LOGIN_START,
        provider,
        properties.isStubMode(),
        !blank(state),
        !blank(redirectUri),
        !blank(codeVerifier));

    if (blank(code)) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE, "소셜 로그인 authorization code가 필요합니다.");
    }

    OAuthStateContext stateContext = verifyState(provider, state);
    String frontendOrigin = stateContext == null ? null : stateContext.frontendOrigin();
    String effectiveRedirectUri =
        blank(redirectUri) ? properties.get(provider).getRedirectUri() : redirectUri.trim();

    SocialProfile profile =
        properties.isStubMode()
            ? stubProfile(provider, code)
            : providerRegistry.get(provider).fetchProfile(code, effectiveRedirectUri, codeVerifier);
    if (blank(profile.providerUserId())) {
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, provider + " 사용자 식별자를 확인할 수 없습니다.");
    }

    UserResolution resolution = resolveOrCreateUser(provider, profile);
    boolean profileComplete = userProfileRepository.existsById(resolution.userId());
    RefreshTokenService.TokenPair tokens =
        refreshTokenService.issueNewSession(resolution.userId().toString(), null);
    safeAudit(resolution.userId().toString(), provider, resolution.newUser(), state);

    log.info(
        LogMessages.Auth.SOCIAL_LOGIN_COMPLETE,
        provider,
        resolution.userId(),
        resolution.newUser(),
        profileComplete);

    return new SocialLoginResult(
        resolution.userId().toString(),
        provider.name(),
        tokens.accessToken(),
        tokens.refreshToken(),
        tokens.accessExpiresInSeconds(),
        tokens.refreshExpiresInSeconds(),
        resolution.newUser(),
        profileComplete,
        frontendOrigin);
  }

  private record UserResolution(Long userId, boolean newUser) {}

  private UserResolution resolveOrCreateUser(SocialProvider provider, SocialProfile profile) {
    String providerUserIdHash = hasher.hash(provider.name() + "|" + profile.providerUserId());
    SocialAccountEntity account =
        socialAccountRepository
            .findByProviderAndProviderUserIdHash(provider.name(), providerUserIdHash)
            .orElse(null);

    if (account == null) {
      return new UserResolution(createNewSocialUser(provider, profile, providerUserIdHash), true);
    }

    Long existingUserId = account.getUserId();
    UserEntity user = userRepository.findById(existingUserId).orElse(null);
    if (user == null) {
      log.warn(
          "Social account points to missing user. Recreating account provider={} userId={} socialAccountId={}",
          provider,
          existingUserId,
          account.getSocialAccountId());
      socialAccountRepository.delete(account);
      socialAccountRepository.flush();
      return new UserResolution(createNewSocialUser(provider, profile, providerUserIdHash), true);
    }

    return new UserResolution(touchExistingAccount(account, user), false);
  }

  private Long createNewSocialUser(
      SocialProvider provider, SocialProfile profile, String providerUserIdHash) {
    UserEntity user = userRepository.save(buildSocialUser(provider, profile));
    Long userId = user.getUserId();

    SocialAccountEntity account = new SocialAccountEntity();
    account.setUserId(userId);
    account.setProvider(provider.name());
    account.setProviderUserIdHash(providerUserIdHash);
    account.setEmailHash(blank(profile.email()) ? null : hasher.hash(profile.email()));
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
    user.setCiHash(hasher.hash("SOCIAL_CI|" + seed));
    user.setCi("SOCIAL_CI|" + seed);
    user.setPhoneHash(hasher.hash("SOCIAL_PHONE|" + seed));
    user.setPhone("UNVERIFIED");
    user.setStatus(UserEntity.UserStatus.ACTIVE);
    user.setFailedLoginCount(0);
    user.setLastLoginAt(now);
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    return user;
  }

  private OAuthStateContext verifyState(SocialProvider provider, String state) {
    if (properties.isStubMode()) {
      return null;
    }
    try {
      return oAuthStateStore.verifyAndConsume(provider.name(), state);
    } catch (IllegalStateException e) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, e.getMessage());
    }
  }

  private SocialProfile stubProfile(SocialProvider provider, String code) {
    String id = provider.name().toLowerCase() + "-" + hasher.hash(code).substring(0, 16);
    return new SocialProfile(id, id + "@stub.burty.local", provider.name() + " 사용자");
  }

  private void safeAudit(String userId, SocialProvider provider, boolean newUser, String state) {
    auditLogger.logSuccess(
        userId,
        "SOCIAL_LOGIN",
        provider.name(),
        "provider=" + provider + ", newUser=" + newUser + ", state=" + truncateForAudit(state));
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private static String truncateForAudit(String state) {
    if (state == null) {
      return "";
    }
    return state.length() <= 80 ? state : state.substring(0, 80);
  }
}
