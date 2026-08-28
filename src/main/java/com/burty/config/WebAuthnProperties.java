/**
 *
 *
 * <pre>
 * <b>Description  : 설정 설정 프로퍼티 (WebAuthnProperties)</b>
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
@ConfigurationProperties(prefix = "burty.webauthn")
public class WebAuthnProperties {
  private String serverSecret = "change-me-webauthn-secret";
  private long challengeTtlSeconds = 300;
  private String rpId = "localhost";
  private String origin = "http://localhost:8080";

  /**
   * 서명 검증을 건너뛰는 스텁 검증기 사용 여부.
   *
   * <p>실제 인증기 없이 이체 흐름을 돌려보기 위한 개발·테스트 전용 설정이다. prod 프로파일에서는 {@code true} 면 기동을 막는다.
   */
  private boolean stubMode = false;

  public String getServerSecret() {
    return serverSecret;
  }

  public void setServerSecret(String serverSecret) {
    this.serverSecret = serverSecret;
  }

  public long getChallengeTtlSeconds() {
    return challengeTtlSeconds;
  }

  public void setChallengeTtlSeconds(long challengeTtlSeconds) {
    this.challengeTtlSeconds = challengeTtlSeconds;
  }

  public String getRpId() {
    return rpId;
  }

  public void setRpId(String rpId) {
    this.rpId = rpId;
  }

  public String getOrigin() {
    return origin;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
  }

  public boolean isStubMode() {
    return stubMode;
  }

  public void setStubMode(boolean stubMode) {
    this.stubMode = stubMode;
  }
}
