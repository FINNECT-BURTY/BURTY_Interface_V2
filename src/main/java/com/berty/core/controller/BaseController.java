package com.berty.core.controller;

import com.berty.core.constants.CommonConstants;
import com.berty.core.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping(CommonConstants.API_BASE_PATH)
public abstract class BaseController {

    /**
     * 성공 응답 생성
     */
    protected <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 성공 응답 생성 (메시지 포함)
     */
    protected <T> ResponseEntity<ApiResponse<T>> success(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(data, message));
    }

    /**
     * 생성 성공 응답 (201 Created)
     */
    protected <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
        return ResponseEntity.status(201).body(ApiResponse.success(data, message));
    }

    /**
     * 페이징 응답 생성
     */
    protected <T> ResponseEntity<ApiResponse<Page<T>>> successPage(Page<T> page) {
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    /**
     * 현재 인증된 사용자명 가져오기
     */
    protected String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * 현재 인증된 사용자 정보 가져오기
     */
    protected Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 헬스체크용 서비스
     */
    @RestController
    @RequestMapping("/health")
    public static class HealthService {

        @GetMapping
        public Map<String, Object> health() {
            return Map.of(
                    "status", "UP",
                    "timestamp", System.currentTimeMillis(),
                    "service", "DDaSum API"
            );
        }

        @GetMapping("/ping")
        public Map<String, String> ping() {
            return Map.of("message", "pong");
        }
    }
}
