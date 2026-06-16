/**
 *
 *
 * <pre>
 * <b>Description  : 설정 설정 프로퍼티 (JwtProperties)</b>
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
@ConfigurationProperties(prefix = "burty.jwt")
public class JwtProperties {
  private String secret = "change-me-jwt-secret-key-for-burty";
  private long expirationSeconds = 3600; // access token: 1h
  private long refreshExpirationSeconds = 604800; // refresh token: 7d

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public long getExpirationSeconds() {
    return expirationSeconds;
  }

  public void setExpirationSeconds(long expirationSeconds) {
    this.expirationSeconds = expirationSeconds;
  }

  public long getRefreshExpirationSeconds() {
    return refreshExpirationSeconds;
  }

  public void setRefreshExpirationSeconds(long refreshExpirationSeconds) {
    this.refreshExpirationSeconds = refreshExpirationSeconds;
  }
}
