/**
 *
 *
 * <pre>
 * <b>Description  : 소셜로그인 (OAuthFrontendRedirect)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.social
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
package com.burty.adapter.in.web.social;

import com.burty.config.BurtyAuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OAuthFrontendRedirect {

  private static final Logger log = LoggerFactory.getLogger(OAuthFrontendRedirect.class);

  private final BurtyAuthProperties authProperties;

  @Value("${FRONTEND_URL:${app.base-url:https://burty.co.kr}}")
  private String defaultFrontendUrl;

  public String resolveFromRequest(HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    String forwarded = request.getHeader("X-Frontend-Origin");
    if (notBlank(forwarded)) {
      return resolveOrigin(forwarded);
    }
    String origin = request.getHeader("Origin");
    if (notBlank(origin)) {
      return resolveOrigin(origin);
    }
    return resolveOrigin(request.getHeader("Referer"));
  }

  public String successUrl(String frontendOrigin, boolean newUser, boolean profileComplete) {
    return buildUrl(frontendOrigin, null, newUser, profileComplete);
  }

  public String safeErrorUrl(String errorCode) {
    try {
      return buildUrl(null, errorCode, null, null);
    } catch (Exception e) {
      log.error("OAuth redirect fallback errorCode={}", errorCode, e);
      String base =
          defaultFrontendUrl == null || defaultFrontendUrl.isBlank() ? "/" : defaultFrontendUrl;
      String safeCode = errorCode == null ? "internal_error" : errorCode;
      return base + "/?error=" + URLEncoder.encode(safeCode, StandardCharsets.UTF_8);
    }
  }

  public String resolveOrigin(String requestedOrigin) {
    if (requestedOrigin == null || requestedOrigin.isBlank()) {
      return null;
    }
    try {
      URI requested = URI.create(requestedOrigin);
      String host = requested.getHost();
      String scheme = requested.getScheme();
      if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) || host == null) {
        return null;
      }
      URI configured =
          defaultFrontendUrl == null || defaultFrontendUrl.isBlank()
              ? null
              : URI.create(defaultFrontendUrl);
      boolean configuredHost =
          configured != null
              && host.equalsIgnoreCase(configured.getHost())
              && scheme.equalsIgnoreCase(configured.getScheme());
      boolean localHost = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
      if (!configuredHost && !localHost) {
        log.warn("Ignoring unsupported frontend origin={}", requestedOrigin);
        return null;
      }
      int port = requested.getPort();
      String authority = port > -1 ? host + ":" + port : host;
      return scheme + "://" + authority;
    } catch (Exception e) {
      log.warn("Ignoring invalid frontend origin={}", requestedOrigin);
      return null;
    }
  }

  private String buildUrl(
      String frontendOrigin, String error, Boolean newUser, Boolean profileComplete) {
    String base =
        frontendOrigin == null || frontendOrigin.isBlank()
            ? (defaultFrontendUrl == null ? "" : defaultFrontendUrl)
            : frontendOrigin;
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(base + authProperties.getOauthSuccessRedirect());
    if (error != null) {
      builder.queryParam("error", error);
    } else {
      if (newUser != null) {
        builder.queryParam("newUser", newUser);
      }
      if (profileComplete != null) {
        builder.queryParam("profileComplete", profileComplete);
      }
    }
    return builder.encode(StandardCharsets.UTF_8).build().toUriString();
  }

  private static boolean notBlank(String value) {
    return value != null && !value.isBlank();
  }
}
