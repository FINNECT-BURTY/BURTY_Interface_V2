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

  public ProdStartupValidator(
      Environment environment,
      MyDataProperties myDataProperties,
      SocialLoginProperties socialLoginProperties,
      ExternalFinanceProperties externalFinanceProperties,
      BurtyApiProperties apiProperties) {
    this.environment = environment;
    this.myDataProperties = myDataProperties;
    this.socialLoginProperties = socialLoginProperties;
    this.externalFinanceProperties = externalFinanceProperties;
    this.apiProperties = apiProperties;
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
