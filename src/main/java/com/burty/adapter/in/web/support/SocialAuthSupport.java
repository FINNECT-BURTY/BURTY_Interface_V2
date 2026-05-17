package com.burty.adapter.in.web.support;

import com.burty.adapter.in.web.dto.SocialLoginResponse;
import com.burty.application.port.in.SocialLoginUseCase;
import com.burty.config.BurtyAuthProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.model.SocialLoginResult;
import com.burty.domain.model.SocialProvider;
import com.burty.security.AuthCookies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Provider 별 AuthController 4개(Kakao/Google/Naver/Apple) 가 공유하는 공통 로직.
 *
 * - BFF callback 의 토큰 교환 + 쿠키 set + FE redirect 흐름
 * - OAuth provider/business 에러 코드 매핑
 * - SPA login 응답 변환
 *
 * Why component: 4개 controller 가 같은 로직을 반복하지 않도록. Inheritance 대신 composition 사용.
 */
@Component
public class SocialAuthSupport {
    private static final Logger log = LoggerFactory.getLogger(SocialAuthSupport.class);

    private final BurtyAuthProperties burtyAuthProperties;
    private final String frontendUrl;

    public SocialAuthSupport(BurtyAuthProperties burtyAuthProperties,
                             @Value("${FRONTEND_URL:${app.base-url:https://burty.co.kr}}") String frontendUrl) {
        this.burtyAuthProperties = burtyAuthProperties;
        this.frontendUrl = frontendUrl;
    }

    /** BFF GET callback 의 표준 처리: code 검증 → 토큰 발급 → 쿠키 set → FE 로 302. */
    public ResponseEntity<Void> handleBffCallback(SocialProvider provider,
                                                  String code,
                                                  String state,
                                                  String error,
                                                  String errorDescription,
                                                  SocialLoginUseCase useCase) {
        log.info("OAuth callback received provider={} hasCode={} hasState={} hasError={}",
                provider, notBlank(code), notBlank(state), notBlank(error));
        try {
            if (notBlank(error)) {
                log.warn("OAuth callback provider error provider={} error={} description={}",
                        provider, error, errorDescription);
                return safeRedirect(mapProviderError(error));
            }
            if (!notBlank(code)) {
                log.warn("OAuth callback missing code provider={}", provider);
                return safeRedirect("missing_code");
            }

            SocialLoginResult result = useCase.login(provider.name(), code, null, state, null);
            ResponseCookie access = buildCookie(AuthCookies.ACCESS, result.getAccessToken(), result.getAccessExpiresInSeconds());
            ResponseCookie refresh = buildCookie(AuthCookies.REFRESH, result.getRefreshToken(), result.getRefreshExpiresInSeconds());

            String location = buildFrontendUrl(null, result.isNewUser(), result.isProfileComplete());
            log.info("OAuth callback success provider={} userId={} newUser={} profileComplete={} redirect={}",
                    provider, result.getUserId(), result.isNewUser(), result.isProfileComplete(), location);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.SET_COOKIE, access.toString())
                    .header(HttpHeaders.SET_COOKIE, refresh.toString())
                    .location(URI.create(location))
                    .build();
        } catch (BusinessException be) {
            String code2 = mapBusinessError(be);
            log.warn("OAuth callback business error provider={} code={} reason={}", provider, code2, be.getMessage());
            return safeRedirect(code2);
        } catch (Exception ex) {
            log.error("OAuth callback unexpected error provider={}", provider, ex);
            return safeRedirect("internal_error");
        }
    }

    /** SPA POST /login 응답 변환. */
    public SocialLoginResponse toResponse(SocialLoginResult result) {
        return new SocialLoginResponse(
                result.getUserId(),
                result.getProvider(),
                result.getAccessToken(),
                result.getRefreshToken(),
                result.getAccessExpiresInSeconds(),
                result.getRefreshExpiresInSeconds(),
                result.isNewUser(),
                result.isProfileComplete()
        );
    }

    public ResponseCookie buildCookie(String name, String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(burtyAuthProperties.isCookieSecure())
                .sameSite(burtyAuthProperties.getCookieSameSite())
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds));
        String domain = burtyAuthProperties.getCookieDomain();
        if (notBlank(domain)) {
            b.domain(domain);
        }
        return b.build();
    }

    /** redirectToFrontend 가 어떤 이유로도 throw 하지 않도록 감싼다. 최후 fallback 으로 frontendUrl 루트로 보낸다. */
    private ResponseEntity<Void> safeRedirect(String errorCode) {
        try {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(buildFrontendUrl(errorCode, null, null)))
                    .build();
        } catch (Exception e) {
            log.error("safeRedirect fallback triggered errorCode={}", errorCode, e);
            String base = frontendUrl == null || frontendUrl.isBlank() ? "/" : frontendUrl;
            try {
                String safeCode = errorCode == null ? "internal_error" : errorCode;
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(base + "/?error=" + URLEncoder.encode(safeCode, StandardCharsets.UTF_8)))
                        .build();
            } catch (Exception inner) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/"))
                        .build();
            }
        }
    }

    private String buildFrontendUrl(String error, Boolean newUser, Boolean profileComplete) {
        String base = frontendUrl == null || frontendUrl.isBlank() ? "" : frontendUrl;
        String path = burtyAuthProperties.getOauthSuccessRedirect();
        // queryParam 이 자체 인코딩하므로 raw 값을 그대로 넘긴다 (double-encode 방지)
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(base + path);
        if (error != null) {
            b.queryParam("error", error);
        } else {
            if (newUser != null) b.queryParam("newUser", newUser);
            if (profileComplete != null) b.queryParam("profileComplete", profileComplete);
        }
        return b.encode(StandardCharsets.UTF_8).build().toUriString();
    }

    private String mapProviderError(String providerError) {
        if (providerError == null) return "internal_error";
        String e = providerError.toLowerCase();
        if (e.contains("access_denied") || e.contains("user_cancelled")) return "user_cancelled";
        if (e.contains("invalid_request") || e.contains("invalid_redirect")) return "invalid_request";
        if (e.contains("server_error") || e.contains("temporarily_unavailable")) return "provider_unavailable";
        return "provider_error";
    }

    private String mapBusinessError(BusinessException be) {
        String message = be.getMessage() == null ? "" : be.getMessage();
        ErrorCode ec = be.getErrorCode();
        if (ec == ErrorCode.INVALID_INPUT_VALUE) {
            if (message.contains("state")) return "state_expired";
            if (message.contains("code")) return "invalid_code";
            if (message.contains("provider") || message.contains("지원")) return "unsupported_provider";
            return "invalid_request";
        }
        if (ec == ErrorCode.EXTERNAL_API_ERROR) return "provider_error";
        if (ec == ErrorCode.INVALID_TOKEN || ec == ErrorCode.EXPIRED_TOKEN) return "invalid_token";
        if (ec == ErrorCode.FORBIDDEN) return "forbidden";
        return "internal_error";
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
