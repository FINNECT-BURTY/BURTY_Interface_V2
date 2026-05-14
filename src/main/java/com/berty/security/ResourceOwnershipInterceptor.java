package com.berty.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

/**
 * URL/쿼리의 {@code userId}가 JWT subject와 일치하는지 검사해 IDOR를 방지합니다.
 * {@code @RequestBody} JSON 내부의 userId는 이 인터셉터 범위 밖이므로, 해당 API는 추후 본문 검증 또는 /me 스타일로 이전하는 것이 좋습니다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ResourceOwnershipInterceptor implements HandlerInterceptor {

    private static final String USER_ID = "userId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String path = request.getRequestURI();
        if (path == null || shouldSkip(path)) {
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

        String requested = resolveRequestedUserId(request);
        if (requested == null || requested.isBlank()) {
            return true;
        }

        if (!ownerId.equals(requested)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "userId does not match authenticated subject");
            return false;
        }
        return true;
    }

    private boolean shouldSkip(String path) {
        return path.startsWith("/api/berty/auth/")
                || path.startsWith("/api/berty/admin/");
    }

    private String resolveRequestedUserId(HttpServletRequest request) {
        String q = request.getParameter(USER_ID);
        if (q != null && !q.isBlank()) {
            return q.trim();
        }
        Object attr = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (attr instanceof Map<?, ?> map) {
            Object v = map.get(USER_ID);
            if (v != null) {
                String s = String.valueOf(v).trim();
                if (!s.isBlank()) {
                    return s;
                }
            }
        }
        return null;
    }
}
