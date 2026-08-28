/**
 *
 *
 * <pre>
 * <b>Description  : 설정 설정 프로퍼티 (BurtySecurityProperties)</b>
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

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Centralized security feature toggles.
 *
 * <p>mode = JWT_FILTER (default): use existing self-issued JWT + JwtAuthenticationFilter mode =
 * RESOURCE_SERVER: validate via OAuth2 Resource Server (requires
 * spring-security-oauth2-resource-server dependency and `issuer-uri` configured). When enabled,
 * JwtAuthenticationFilter is bypassed.
 */
@Configuration
@ConfigurationProperties(prefix = "burty.security")
public class BurtySecurityProperties {

  private Mode mode = Mode.JWT_FILTER;
  private final ResourceServer resourceServer = new ResourceServer();
  private final Cors cors = new Cors();
  private final Actuator actuator = new Actuator();

  /**
   * 전달 헤더를 신뢰할 프록시 대역(CIDR).
   *
   * <p>비워두면 {@code X-Forwarded-For} 등을 아예 보지 않고 실제 접속 출처만 쓴다. 프록시가 없는 배포에서 안전한 기본값이다. 로드밸런서·인그레스 뒤에
   * 둔다면 그 대역을 여기에 적어야 클라이언트 IP 가 제대로 잡힌다.
   */
  private List<String> trustedProxies = new ArrayList<>();

  public List<String> getTrustedProxies() {
    return trustedProxies;
  }

  public void setTrustedProxies(List<String> trustedProxies) {
    this.trustedProxies = trustedProxies;
  }

  public Mode getMode() {
    return mode;
  }

  public void setMode(Mode mode) {
    this.mode = mode;
  }

  public ResourceServer getResourceServer() {
    return resourceServer;
  }

  public Cors getCors() {
    return cors;
  }

  public Actuator getActuator() {
    return actuator;
  }

  public boolean isResourceServerEnabled() {
    return mode == Mode.RESOURCE_SERVER && resourceServer.isEnabled();
  }

  public enum Mode {
    JWT_FILTER,
    RESOURCE_SERVER
  }

  public static class ResourceServer {
    private boolean enabled = false;
    private String issuerUri;
    private String jwkSetUri;
    private String audience;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getIssuerUri() {
      return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
      this.issuerUri = issuerUri;
    }

    public String getJwkSetUri() {
      return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
      this.jwkSetUri = jwkSetUri;
    }

    public String getAudience() {
      return audience;
    }

    public void setAudience(String audience) {
      this.audience = audience;
    }
  }

  /**
   * CORS allowedOrigins / methods / headers. 운영은 same-origin 이라 비워두면 CORS 자체가 비활성. dev FE 가 다른
   * origin 에서 호출하면 채워서 활성화.
   */
  public static class Cors {
    /** allowedOriginPatterns 로 들어감 — 와일드카드(*) 사용 가능. 비어 있으면 CORS 비활성. */
    private List<String> allowedOrigins = new ArrayList<>();

    private List<String> allowedMethods =
        List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private List<String> allowedHeaders = List.of("*");
    private List<String> exposedHeaders = List.of();
    private boolean allowCredentials = true;
    private long maxAgeSeconds = 3600;

    public List<String> getAllowedOrigins() {
      return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
      this.allowedOrigins = allowedOrigins;
    }

    public List<String> getAllowedMethods() {
      return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
      this.allowedMethods = allowedMethods;
    }

    public List<String> getAllowedHeaders() {
      return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
      this.allowedHeaders = allowedHeaders;
    }

    public List<String> getExposedHeaders() {
      return exposedHeaders;
    }

    public void setExposedHeaders(List<String> exposedHeaders) {
      this.exposedHeaders = exposedHeaders;
    }

    public boolean isAllowCredentials() {
      return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
      this.allowCredentials = allowCredentials;
    }

    public long getMaxAgeSeconds() {
      return maxAgeSeconds;
    }

    public void setMaxAgeSeconds(long maxAgeSeconds) {
      this.maxAgeSeconds = maxAgeSeconds;
    }

    public boolean isEnabled() {
      return allowedOrigins != null && !allowedOrigins.isEmpty();
    }
  }

  /** Actuator / Prometheus 노출 정책. */
  public static class Actuator {
    /** false 이면 /actuator/** 는 ADMIN 역할 필요 (health liveness 제외). */
    private boolean prometheusPermitAll = true;

    private String internalToken = "";

    public boolean isPrometheusPermitAll() {
      return prometheusPermitAll;
    }

    public void setPrometheusPermitAll(boolean prometheusPermitAll) {
      this.prometheusPermitAll = prometheusPermitAll;
    }

    public String getInternalToken() {
      return internalToken;
    }

    public void setInternalToken(String internalToken) {
      this.internalToken = internalToken;
    }
  }
}
