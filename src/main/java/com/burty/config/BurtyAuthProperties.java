/**
 *
 *
 * <pre>
 * <b>Description  : 설정 설정 프로퍼티 (BurtyAuthProperties)</b>
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

/** 인증 관련 플래그. 운영에서는 테스트용 JWT 발급을 끄는 것이 기본입니다. */
@Component
@ConfigurationProperties(prefix = "burty.auth")
public class BurtyAuthProperties {

  /**
   * {@code POST /api/v1/auth/token} 허용 여부. {@code spring.profiles.active}에 prod가 있으면 무조건 비활성화됩니다.
   */
  private boolean testTokenEnabled = false;

  /**
   * BFF 콜백 핸들러가 OAuth 성공/실패 후 브라우저를 redirect 시킬 FE URL. 예: https://burty.co.kr/auth/callback —
   * query 로 newUser, profileComplete, error 가 첨부됨. 빈 값이면 FRONTEND_URL (또는 application properties 의
   * default) 를 그대로 사용.
   */
  private String oauthSuccessRedirect = "/auth/callback";

  /** Cookie 도메인 — COOKIE_DOMAIN env 우선, 없으면 빈 문자열(현재 호스트). */
  private String cookieDomain = "";

  /** Cookie Secure flag. HTTPS 환경에선 true. */
  private boolean cookieSecure = true;

  /** Cookie SameSite — Lax (OAuth top-level redirect 시 쿠키 첨부 필요) 또는 None. */
  private String cookieSameSite = "Lax";

  public boolean isTestTokenEnabled() {
    return testTokenEnabled;
  }

  public void setTestTokenEnabled(boolean testTokenEnabled) {
    this.testTokenEnabled = testTokenEnabled;
  }

  public String getOauthSuccessRedirect() {
    return oauthSuccessRedirect;
  }

  public void setOauthSuccessRedirect(String oauthSuccessRedirect) {
    this.oauthSuccessRedirect = oauthSuccessRedirect;
  }

  public String getCookieDomain() {
    return cookieDomain;
  }

  public void setCookieDomain(String cookieDomain) {
    this.cookieDomain = cookieDomain;
  }

  public boolean isCookieSecure() {
    return cookieSecure;
  }

  public void setCookieSecure(boolean cookieSecure) {
    this.cookieSecure = cookieSecure;
  }

  public String getCookieSameSite() {
    return cookieSameSite;
  }

  public void setCookieSameSite(String cookieSameSite) {
    this.cookieSameSite = cookieSameSite;
  }
}
