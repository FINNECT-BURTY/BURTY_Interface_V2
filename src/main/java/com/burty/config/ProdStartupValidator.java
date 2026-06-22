/**
 *
 *
 * <pre>
 * <b>Description  : 설정 검증기 (ProdStartupValidator)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.config
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

  private final Environment environment;
  private final MyDataProperties myDataProperties;
  private final SocialLoginProperties socialLoginProperties;
  private final ExternalFinanceProperties externalFinanceProperties;
  private final BurtyApiProperties apiProperties;
  private final IdentityProperties identityProperties;
  private final NotifyProperties notifyProperties;

  public ProdStartupValidator(
      Environment environment,
      MyDataProperties myDataProperties,
      SocialLoginProperties socialLoginProperties,
      ExternalFinanceProperties externalFinanceProperties,
      BurtyApiProperties apiProperties,
      IdentityProperties identityProperties,
      NotifyProperties notifyProperties) {
    this.environment = environment;
    this.myDataProperties = myDataProperties;
    this.socialLoginProperties = socialLoginProperties;
    this.externalFinanceProperties = externalFinanceProperties;
    this.apiProperties = apiProperties;
    this.identityProperties = identityProperties;
    this.notifyProperties = notifyProperties;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void validate() {
    if (!isProdProfile()) {
      return;
    }

    String jwt = environment.getProperty("burty.jwt.secret", DEFAULT_JWT);
    String sign = environment.getProperty("burty.sign.secret", DEFAULT_SIGN);
    String admin = environment.getProperty("burty.admin.setup-key", DEFAULT_ADMIN);
    boolean redisEnabled =
        environment.getProperty("burty.redis.enabled", Boolean.class, Boolean.FALSE);

    if (containsDefaultSecret(jwt) || containsDefaultSecret(sign) || containsDefaultSecret(admin)) {
      throw new IllegalStateException(
          "PROD startup blocked: JWT/sign/admin secrets must not use default values.");
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
    if (apiProperties.isSwaggerEnabled()) {
      throw new IllegalStateException(
          "PROD startup blocked: burty.api.swagger-enabled must be false.");
    }
    if (!identityProperties.isStubMode()) {
      if (identityProperties.getProvider() == IdentityProperties.Provider.NICE
          && !identityProperties.getNice().isConfigured()) {
        throw new IllegalStateException(
            "PROD startup blocked: NICE identity credentials required when stub-mode=false.");
      }
      if (identityProperties.getProvider() == IdentityProperties.Provider.KCB
          && !identityProperties.getKcb().isConfigured()) {
        throw new IllegalStateException(
            "PROD startup blocked: KCB identity credentials required when stub-mode=false.");
      }
    }
    if (!notifyProperties.getEmail().isStubMode()
        && (environment.getProperty("spring.mail.host", "").isBlank())) {
      throw new IllegalStateException(
          "PROD startup blocked: MAIL_HOST required when email stub-mode=false.");
    }
    if (!notifyProperties.getSms().isStubMode() && !notifyProperties.getSms().isConfigured()) {
      throw new IllegalStateException(
          "PROD startup blocked: SMS API credentials required when sms stub-mode=false.");
    }
    if (!notifyProperties.getPush().isStubMode() && !notifyProperties.getPush().isConfigured()) {
      throw new IllegalStateException(
          "PROD startup blocked: FCM credentials required when push stub-mode=false.");
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
