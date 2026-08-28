package com.burty.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** prod 프로파일에서 위험한 기본값·stub 설정이면 기동을 중단합니다. */
@Component
public class ProdStartupValidator {

  private static final String DEFAULT_JWT = "change-me-jwt-secret-key-for-burty";
  private static final String DEFAULT_SIGN = "change-me-burty-sign-secret";
  private static final String DEFAULT_ADMIN = "burty-admin-setup-key";
  private static final String DEFAULT_FIELD_ENCRYPTION = "change-me-burty-field-encryption-key-32";

  private final Environment environment;
  private final MyDataProperties myDataProperties;
  private final SocialLoginProperties socialLoginProperties;
  private final ExternalFinanceProperties externalFinanceProperties;
  private final BurtyApiProperties apiProperties;
  private final IdentityProperties identityProperties;
  private final NotifyProperties notifyProperties;
  private final BurtySecurityProperties securityProperties;

  public ProdStartupValidator(
      Environment environment,
      MyDataProperties myDataProperties,
      SocialLoginProperties socialLoginProperties,
      ExternalFinanceProperties externalFinanceProperties,
      BurtyApiProperties apiProperties,
      IdentityProperties identityProperties,
      NotifyProperties notifyProperties,
      BurtySecurityProperties securityProperties) {
    this.environment = environment;
    this.myDataProperties = myDataProperties;
    this.socialLoginProperties = socialLoginProperties;
    this.externalFinanceProperties = externalFinanceProperties;
    this.apiProperties = apiProperties;
    this.identityProperties = identityProperties;
    this.notifyProperties = notifyProperties;
    this.securityProperties = securityProperties;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void validate() {
    if (!isProdProfile()) {
      return;
    }

    String jwt = environment.getProperty("burty.jwt.secret", DEFAULT_JWT);
    String sign = environment.getProperty("burty.sign.secret", DEFAULT_SIGN);
    String admin = environment.getProperty("burty.admin.setup-key", DEFAULT_ADMIN);
    String fieldEncryption =
        environment.getProperty("burty.security.field-encryption-key", DEFAULT_FIELD_ENCRYPTION);
    boolean redisEnabled =
        environment.getProperty("burty.redis.enabled", Boolean.class, Boolean.FALSE);

    if (containsDefaultSecret(jwt) || containsDefaultSecret(sign) || containsDefaultSecret(admin)) {
      throw new IllegalStateException(
          "PROD startup blocked: JWT/sign/admin secrets must not use default values.");
    }
    if (containsDefaultSecret(fieldEncryption)) {
      throw new IllegalStateException(
          "PROD startup blocked: burty.security.field-encryption-key must be set to a unique value.");
    }
    if (!redisEnabled) {
      throw new IllegalStateException(
          "PROD startup blocked: burty.redis.enabled must be true for shared auth state.");
    }
    if (myDataProperties.isStubMode()) {
      throw new IllegalStateException(
          "PROD startup blocked: burty.mydata.stub-mode must be false.");
    }
    if (socialLoginProperties.isStubMode()) {
      throw new IllegalStateException(
          "PROD startup blocked: burty.social.stub-mode must be false.");
    }
    if (externalFinanceProperties.isStubMode()) {
      throw new IllegalStateException(
          "PROD startup blocked: burty.external.stub-mode must be false.");
    }
    if (environment.getProperty("burty.admin.bootstrap-enabled", Boolean.class, Boolean.FALSE)) {
      throw new IllegalStateException(
          "PROD startup blocked: burty.admin.bootstrap-enabled must be false "
              + "(무인증 관리자 등록 창구는 운영에서 열려 있으면 안 됩니다).");
    }
    if (apiProperties.isSwaggerEnabled()) {
      throw new IllegalStateException(
          "PROD startup blocked: burty.api.swagger-enabled must be false.");
    }
    if (securityProperties.getTrustedProxies().isEmpty()) {
      throw new IllegalStateException(
          "PROD startup blocked: burty.security.trusted-proxies must list the ingress/LB CIDRs "
              + "(비워두면 모든 요청이 프록시 IP 로 기록되어 레이트리밋과 접근 기록이 무의미해집니다).");
    }
    if (environment.getProperty("burty.webauthn.stub-mode", Boolean.class, Boolean.FALSE)) {
      throw new IllegalStateException(
          "PROD startup blocked: burty.webauthn.stub-mode must be false "
              + "(서명을 검증하지 않는 스텁으로 이체의 생체인증 게이트가 무력화됩니다).");
    }
    if (identityProperties.isStubMode()) {
      throw new IllegalStateException(
          "PROD startup blocked: burty.identity.stub-mode must be false.");
    }
    if (identityProperties.getProvider() == IdentityProperties.Provider.NICE
        && !identityProperties.getNice().isConfigured()) {
      throw new IllegalStateException("PROD startup blocked: NICE identity credentials required.");
    }
    if (identityProperties.getProvider() == IdentityProperties.Provider.KCB
        && !identityProperties.getKcb().isConfigured()) {
      throw new IllegalStateException("PROD startup blocked: KCB identity credentials required.");
    }
    if (notifyProperties.getEmail().isStubMode()) {
      throw new IllegalStateException(
          "PROD startup blocked: burty.notify.email.stub-mode must be false.");
    }
    if (notifyProperties.getSms().isStubMode()) {
      throw new IllegalStateException(
          "PROD startup blocked: burty.notify.sms.stub-mode must be false.");
    }
    if (notifyProperties.getPush().isStubMode()) {
      throw new IllegalStateException(
          "PROD startup blocked: burty.notify.push.stub-mode must be false.");
    }
    if (environment.getProperty("spring.mail.host", "").isBlank()) {
      throw new IllegalStateException("PROD startup blocked: MAIL_HOST is required.");
    }
    if (!notifyProperties.getSms().isConfigured()) {
      throw new IllegalStateException("PROD startup blocked: SMS API credentials required.");
    }
    if (!notifyProperties.getPush().isConfigured()) {
      throw new IllegalStateException("PROD startup blocked: FCM credentials required.");
    }
  }

  private static boolean containsDefaultSecret(String value) {
    if (value == null || value.isBlank()) {
      return true;
    }
    String normalized = value.toLowerCase();
    return normalized.contains("change-me") || normalized.equals(DEFAULT_ADMIN);
  }

  private boolean isProdProfile() {
    for (String profile : environment.getActiveProfiles()) {
      if ("prod".equalsIgnoreCase(profile)) {
        return true;
      }
    }
    return false;
  }
}
