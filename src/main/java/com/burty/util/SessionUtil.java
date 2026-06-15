/**
 *
 *
 * <pre>
 * <b>Description  : 유틸 (SessionUtil)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.util
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
package com.burty.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** JWT(Spring Security) 기반 현재 사용자 조회. chocopie HttpSession SessionUser 대체. */
@Component
public class SessionUtil {

  public String getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken
        || authentication.getPrincipal() == null
        || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
      return null;
    }
    return String.valueOf(authentication.getPrincipal());
  }

  public boolean isLoggedIn() {
    return getCurrentUserId() != null;
  }

  public boolean hasRole(String role) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return false;
    }
    String normalized = role.startsWith("ROLE_") ? role : "ROLE_" + role;
    for (GrantedAuthority authority : authentication.getAuthorities()) {
      if (normalized.equals(authority.getAuthority())) {
        return true;
      }
    }
    return false;
  }
}
