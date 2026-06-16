/**
 *
 *
 * <pre>
 * <b>Description  : 보안 인터셉터 (RequestBodyOwnershipInterceptor)</b>
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

/** JSON body 의 userId / parentUserId 가 JWT subject 와 일치하는지 검사합니다. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 25)
public class RequestBodyOwnershipInterceptor implements HandlerInterceptor {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (!(request instanceof ContentCachingRequestWrapper wrapper)) {
      return true;
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return true;
    }
    Object principal = authentication.getPrincipal();
    if (!(principal instanceof String ownerId) || ownerId.isBlank()) {
      return true;
    }

    JsonNode body = readJsonBody(wrapper);
    if (body == null) {
      return true;
    }

    if (mismatch(ownerId, body.get("userId"))) {
      response.sendError(
          HttpServletResponse.SC_FORBIDDEN, "userId does not match authenticated subject");
      return false;
    }
    if (mismatch(ownerId, body.get("parentUserId"))) {
      response.sendError(
          HttpServletResponse.SC_FORBIDDEN, "parentUserId does not match authenticated subject");
      return false;
    }
    return true;
  }

  private static boolean mismatch(String ownerId, JsonNode field) {
    if (field == null || field.isNull()) {
      return false;
    }
    String requested = field.asText("").trim();
    return !requested.isEmpty() && !ownerId.equals(requested);
  }

  private static JsonNode readJsonBody(ContentCachingRequestWrapper wrapper) {
    try {
      if (wrapper.getContentAsByteArray().length == 0) {
        wrapper.getInputStream().readAllBytes();
      }
      byte[] buf = wrapper.getContentAsByteArray();
      if (buf.length == 0) {
        return null;
      }
      return MAPPER.readTree(new String(buf, StandardCharsets.UTF_8));
    } catch (Exception e) {
      return null;
    }
  }
}
