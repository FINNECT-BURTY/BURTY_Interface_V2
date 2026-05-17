package com.burty.adapter.in.web;

import com.burty.adapter.in.web.dto.RefreshTokenRequest;
import com.burty.adapter.in.web.dto.SocialLoginRequest;
import com.burty.adapter.in.web.dto.SocialLoginResponse;
import com.burty.adapter.in.web.dto.TokenIssueRequest;
import com.burty.adapter.in.web.dto.TokenPairResponse;
import com.burty.adapter.in.web.dto.TokenResponse;
import com.burty.adapter.in.web.dto.AuthorizeUrlResponse;
import com.burty.adapter.in.web.dto.LogoutResponse;
import com.burty.application.port.in.SocialLoginUseCase;
import com.burty.config.BurtyAuthProperties;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.model.SocialLoginResult;
import com.burty.security.AuthCookies;
import com.burty.security.JwtBlacklistService;
import com.burty.security.JwtTokenProvider;
import com.burty.security.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@Tag(name = "BURTY Auth", description = "BURTY 인증(JWT 발급/Refresh/로그아웃) API")
public class AuthController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtBlacklistService jwtBlacklistService;
    private final RefreshTokenService refreshTokenService;
    private final SocialLoginUseCase socialLoginUseCase;
    private final BurtyAuthProperties burtyAuthProperties;
    private final Environment environment;
    private final String frontendUrl;

    public AuthController(JwtTokenProvider jwtTokenProvider,
                          JwtBlacklistService jwtBlacklistService,
                          RefreshTokenService refreshTokenService,
                          SocialLoginUseCase socialLoginUseCase,
                          BurtyAuthProperties burtyAuthProperties,
                          Environment environment,
                          @Value("${FRONTEND_URL:${app.base-url:https://burty.co.kr}}") String frontendUrl) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtBlacklistService = jwtBlacklistService;
        this.refreshTokenService = refreshTokenService;
        this.socialLoginUseCase = socialLoginUseCase;
        this.burtyAuthProperties = burtyAuthProperties;
        this.environment = environment;
        this.frontendUrl = frontendUrl;
    }

    @PostMapping("/token")
    @Operation(
            summary = "JWT 발급 (테스트용)",
            description = "userId 기반으로 접근 토큰을 발급합니다. `burty.auth.test-token-enabled=true`이고 `prod` 프로파일이 아닐 때만 허용됩니다.",
            security = {}
    )
    public ApiResponse<TokenResponse> issueToken(@RequestBody TokenIssueRequest request) {
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")
                || !burtyAuthProperties.isTestTokenEnabled()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "테스트용 JWT 발급이 비활성화되어 있습니다. (burty.auth.test-token-enabled, prod 프로파일)");
        }
        String userId = request.getUserId() != null ? request.getUserId() : UUID.randomUUID().toString();
        String token = jwtTokenProvider.generateToken(userId);
        return ApiResponse.ok(new TokenResponse(token));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Access / Refresh token 재발급",
            description = "유효한 refresh token 으로 access + refresh 쌍을 재발급합니다. 매 호출 시 refresh token 도 회전(rotation)됩니다. 이미 revoke 된 refresh token 이 들어오면 도난 의심으로 해당 사용자의 모든 세션이 강제 종료됩니다.",
            security = {}
    )
    public ApiResponse<TokenPairResponse> refresh(@RequestBody RefreshTokenRequest request) {
        RefreshTokenService.TokenPair pair = refreshTokenService.rotate(request.getRefreshToken());
        return ApiResponse.ok(new TokenPairResponse(
                pair.accessToken(),
                pair.refreshToken(),
                pair.accessExpiresInSeconds(),
                pair.refreshExpiresInSeconds()
        ));
    }

    @GetMapping("/social/{provider}/authorize-url")
    @Operation(
            summary = "소셜 로그인 인가 URL 생성",
            description = "GOOGLE/KAKAO/NAVER/APPLE OAuth 인가 URL과 state를 반환합니다. " +
                    "`redirectUri` 를 query param 으로 넘기면 그 값을 OAuth provider 에 전달 — " +
                    "단 해당 URI 가 provider 콘솔에 사전 등록되어 있어야 함 (등록 안 되면 카카오 KOE006 등). " +
                    "넘기지 않으면 application properties 의 redirect-uri default 사용. " +
                    "`burty.social.stub-mode=false`일 때 로그인 요청의 state 와 redirectUri 는 여기서 받은 값을 그대로 다시 보내야 함.",
            security = {}
    )
    public ApiResponse<AuthorizeUrlResponse> socialAuthorizeUrl(@PathVariable String provider,
                                                                @RequestParam(required = false) String state,
                                                                @RequestParam(required = false) String redirectUri) {
        var auth = socialLoginUseCase.createAuthorizeUrl(provider, state, redirectUri);
        return ApiResponse.ok(new AuthorizeUrlResponse(auth.authorizeUrl(), auth.state()));
    }

    @PostMapping("/social/{provider}/login")
    @Operation(
            summary = "소셜 로그인 (SPA / API client 용)",
            description = "GOOGLE/KAKAO/NAVER/APPLE authorization code를 검증하고 BURTY access + refresh token 쌍을 응답 body 로 반환합니다. SPA 가 FE 콜백 페이지에서 code 를 수신해 호출하는 패턴.",
            security = {}
    )
    public ApiResponse<SocialLoginResponse> socialLogin(@PathVariable String provider,
                                                        @RequestBody SocialLoginRequest request) {
        SocialLoginResult result = socialLoginUseCase.login(
                provider,
                request.getCode(),
                request.getRedirectUri(),
                request.getState(),
                request.getCodeVerifier()
        );
        return ApiResponse.ok(new SocialLoginResponse(
                result.getUserId(),
                result.getProvider(),
                result.getAccessToken(),
                result.getRefreshToken(),
                result.getAccessExpiresInSeconds(),
                result.getRefreshExpiresInSeconds(),
                result.isNewUser(),
                result.isProfileComplete()
        ));
    }

    /**
     * OAuth provider 가 redirect 하는 GET callback. BFF 패턴.
     * - 토큰 교환 후 access + refresh 를 HttpOnly Secure SameSite=Lax 쿠키로 set
     * - FE 의 onboarding/landing 페이지로 302 redirect
     *
     * redirect_uri 는 application properties 의 burty.social.{provider}.redirect-uri default 값을 사용
     * (FE 가 authorize-url 호출 시 redirectUri 를 안 넘긴 경우와 동일). 이 endpoint 자체가 그 default 의 종착지.
     */
    @GetMapping("/social/{provider}/callback")
    @Operation(
            summary = "소셜 로그인 BFF 콜백",
            description = "OAuth provider 가 redirect 하는 GET 엔드포인트. 토큰 교환 후 HttpOnly 쿠키 설정 + FE 페이지로 302.",
            security = {}
    )
    public ResponseEntity<Void> socialCallback(@PathVariable String provider,
                                               @RequestParam(required = false) String code,
                                               @RequestParam(required = false) String state,
                                               @RequestParam(required = false) String error,
                                               @RequestParam(required = false, name = "error_description") String errorDescription) {
        try {
            // 1. provider 가 error 응답을 redirect 로 알린 경우 (사용자가 동의 거부 등)
            if (error != null && !error.isBlank()) {
                log.warn("OAuth callback provider error provider={} error={} description={}",
                        provider, error, errorDescription);
                return redirectToFrontend(mapProviderError(error), null, null);
            }
            if (code == null || code.isBlank()) {
                log.warn("OAuth callback missing code provider={}", provider);
                return redirectToFrontend("missing_code", null, null);
            }

            SocialLoginResult result = socialLoginUseCase.login(provider, code, null, state, null);

            ResponseCookie accessCookie = buildCookie(AuthCookies.ACCESS, result.getAccessToken(), result.getAccessExpiresInSeconds());
            ResponseCookie refreshCookie = buildCookie(AuthCookies.REFRESH, result.getRefreshToken(), result.getRefreshExpiresInSeconds());

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .location(java.net.URI.create(buildFrontendUrl(null, result.isNewUser(), result.isProfileComplete())))
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
        if (ec == ErrorCode.EXTERNAL_API_ERROR) {
            return "provider_error";
        }
        if (ec == ErrorCode.INVALID_TOKEN || ec == ErrorCode.EXPIRED_TOKEN) {
            return "invalid_token";
        }
        if (ec == ErrorCode.FORBIDDEN) {
            return "forbidden";
        }
        return "internal_error";
    }

    /**
     * redirectToFrontend 가 어떤 이유로도 throw 하지 않도록 감싼다 (URI 생성 실패 등).
     * 최후 fallback 으로 frontendUrl 의 루트로 보낸다.
     */
    private ResponseEntity<Void> safeRedirect(String errorCode) {
        try {
            return redirectToFrontend(errorCode, null, null);
        } catch (Exception e) {
            log.error("safeRedirect fallback triggered errorCode={}", errorCode, e);
            String base = frontendUrl == null || frontendUrl.isBlank() ? "/" : frontendUrl;
            try {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(java.net.URI.create(base + "/?error=" + URLEncoder.encode(errorCode == null ? "internal_error" : errorCode, StandardCharsets.UTF_8)))
                        .build();
            } catch (Exception inner) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(java.net.URI.create("/"))
                        .build();
            }
        }
    }

    private ResponseEntity<Void> redirectToFrontend(String error, Boolean newUser, Boolean profileComplete) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(java.net.URI.create(buildFrontendUrl(error, newUser, profileComplete)))
                .build();
    }

    private String buildFrontendUrl(String error, Boolean newUser, Boolean profileComplete) {
        String base = frontendUrl == null || frontendUrl.isBlank() ? "" : frontendUrl;
        String path = burtyAuthProperties.getOauthSuccessRedirect();
        // UriComponentsBuilder.queryParam 이 자체적으로 인코딩하므로 raw 값을 그대로 넘긴다 (double-encode 방지)
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(base + path);
        if (error != null) {
            b.queryParam("error", error);
        } else {
            if (newUser != null) b.queryParam("newUser", newUser);
            if (profileComplete != null) b.queryParam("profileComplete", profileComplete);
        }
        return b.encode(StandardCharsets.UTF_8).build().toUriString();
    }

    private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(burtyAuthProperties.isCookieSecure())
                .sameSite(burtyAuthProperties.getCookieSameSite())
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds));
        String domain = burtyAuthProperties.getCookieDomain();
        if (domain != null && !domain.isBlank()) {
            b.domain(domain);
        }
        return b.build();
    }

    @PostMapping("/logout")
    @Operation(
            summary = "로그아웃",
            description = "현재 access token 을 블랙리스트에 등록하고, body 로 받은 refresh token 을 revoke. 쿠키도 즉시 만료.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                              @RequestBody(required = false) RefreshTokenRequest request) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwtBlacklistService.blacklist(authHeader.substring(7));
        }
        if (request != null) {
            refreshTokenService.revoke(request.getRefreshToken());
        }
        ResponseCookie expireAccess = buildCookie(AuthCookies.ACCESS, "", 0);
        ResponseCookie expireRefresh = buildCookie(AuthCookies.REFRESH, "", 0);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expireAccess.toString())
                .header(HttpHeaders.SET_COOKIE, expireRefresh.toString())
                .body(ApiResponse.ok(new LogoutResponse(true)));
    }
}
