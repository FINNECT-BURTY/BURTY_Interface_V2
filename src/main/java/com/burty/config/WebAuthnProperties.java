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

  /**
   * 브라우저가 {@code clientDataJSON} 에 넣는 origin 과 비교되는 값.
   *
   * <p><b>패스키를 호출하는 프론트엔드의 origin 이다.</b> 백엔드 API 주소가 아니다. 예전에 이 값이 {@code app.base-url}(백엔드 URL)에
   * 묶여 있었고, prod 는 FE·BE 가 같은 도메인이라 우연히 맞아떨어져 문제가 드러나지 않았다.
   */
  private String origin = "http://localhost:3000";

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
