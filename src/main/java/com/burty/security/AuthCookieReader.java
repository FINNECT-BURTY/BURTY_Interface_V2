/**
 *
 *
 * <pre>
 * <b>Description  : 보안 (AuthCookieReader)</b>
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

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public final class AuthCookieReader {

  private AuthCookieReader() {}

  public static String read(HttpServletRequest request, String name) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    for (Cookie cookie : cookies) {
      if (name.equals(cookie.getName())
          && cookie.getValue() != null
          && !cookie.getValue().isBlank()) {
        return cookie.getValue();
      }
    }
    return null;
  }
}
