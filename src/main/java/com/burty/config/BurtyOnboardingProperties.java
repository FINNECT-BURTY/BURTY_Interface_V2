/**
 *
 *
 * <pre>
 * <b>Description  : 설정 설정 프로퍼티 (BurtyOnboardingProperties)</b>
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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "burty.onboarding.consent")
public class BurtyOnboardingProperties {

  private String termsVersion = "1.0";
  private String privacyVersion = "1.0";

  public String getTermsVersion() {
    return termsVersion;
  }

  public void setTermsVersion(String termsVersion) {
    this.termsVersion = termsVersion;
  }

  public String getPrivacyVersion() {
    return privacyVersion;
  }

  public void setPrivacyVersion(String privacyVersion) {
    this.privacyVersion = privacyVersion;
  }
}
