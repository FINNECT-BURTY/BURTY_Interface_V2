package com.nuri.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthLevelInterceptor implements HandlerInterceptor {
    private final RiskProofService riskProofService;

    public AuthLevelInterceptor(RiskProofService riskProofService) {
        this.riskProofService = riskProofService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        AuthLevel authLevel = handlerMethod.getMethodAnnotation(AuthLevel.class);
        if (authLevel == null) {
            authLevel = handlerMethod.getBeanType().getAnnotation(AuthLevel.class);
        }
        if (authLevel == null) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Bearer token is required");
            return false;
        }
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication is required");
            return false;
        }
        String userId = String.valueOf(SecurityContextHolder.getContext().getAuthentication().getPrincipal());

        if (authLevel.value() == RiskLevel.LEVEL_2) {
            String riskProof = request.getHeader("X-Risk-Proof");
            if (!riskProofService.verify(riskProof, userId, RiskLevel.LEVEL_2)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Verified level-2 risk proof is required");
                return false;
            }
        }

        if (authLevel.value() == RiskLevel.LEVEL_3) {
            String riskProof = request.getHeader("X-Risk-Proof");
            if (!riskProofService.verify(riskProof, userId, RiskLevel.LEVEL_3)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Verified level-3 risk proof is required");
                return false;
            }
        }

        return true;
    }
}
