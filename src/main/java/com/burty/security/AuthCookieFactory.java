/**
 *
 *
 * <pre>
 * <b>Description  : 보안 (AuthCookieFactory)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.security
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
package com.burty.security;

import com.burty.config.BurtyAuthProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthCookieFactory {

  private final BurtyAuthProperties authProperties;

  public ResponseCookie sessionCookie(String name, String value, long maxAgeSeconds) {
    ResponseCookie.ResponseCookieBuilder builder =
        ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(authProperties.isCookieSecure())
            .sameSite(authProperties.getCookieSameSite())
            .path("/")
            .maxAge(Duration.ofSeconds(maxAgeSeconds));
    String domain = authProperties.getCookieDomain();
    if (domain != null && !domain.isBlank()) {
      builder.domain(domain);
    }
    return builder.build();
  }

  public ResponseCookie expire(String name) {
    return sessionCookie(name, "", 0);
  }
}
