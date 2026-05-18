package com.burty.adapter.in.web;

import com.burty.adapter.in.web.dto.LogoutResponse;
import com.burty.adapter.in.web.dto.RefreshTokenRequest;
import com.burty.adapter.in.web.dto.TokenIssueRequest;
import com.burty.adapter.in.web.dto.TokenPairResponse;
import com.burty.adapter.in.web.dto.TokenResponse;
import com.burty.adapter.in.web.support.SocialAuthSupport;
import com.burty.config.BurtyAuthProperties;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.security.AuthCookieReader;
import com.burty.security.AuthCookies;
import com.burty.security.JwtBlacklistService;
import com.burty.security.JwtTokenProvider;
import com.burty.security.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.UUID;

/**
 * BURTY 인증 기본 endpoint (JWT 발급/refresh/logout).
 *
 * 소셜 로그인은 provider 별 controller 에 분리되어 있다:
 *   - {@link KakaoAuthController}
 *   - {@link GoogleAuthController}
 *   - {@link NaverAuthController}
 *   - {@link AppleAuthController}
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "BURTY Auth", description = "BURTY 인증(JWT 발급/Refresh/로그아웃) API")
public class AuthController extends BaseController {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtBlacklistService jwtBlacklistService;
    private final RefreshTokenService refreshTokenService;
    private final BurtyAuthProperties burtyAuthProperties;
    private final Environment environment;
    private final SocialAuthSupport socialAuthSupport;

    public AuthController(JwtTokenProvider jwtTokenProvider,
                          JwtBlacklistService jwtBlacklistService,
                          RefreshTokenService refreshTokenService,
                          BurtyAuthProperties burtyAuthProperties,
                          Environment environment,
                          SocialAuthSupport socialAuthSupport) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtBlacklistService = jwtBlacklistService;
        this.refreshTokenService = refreshTokenService;
        this.burtyAuthProperties = burtyAuthProperties;
        this.environment = environment;
        this.socialAuthSupport = socialAuthSupport;
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
        return ApiResponse.ok(new TokenResponse(jwtTokenProvider.generateToken(userId)));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Access / Refresh token 재발급",
            description = "refresh token 을 body 또는 HttpOnly 쿠키(BURTY_REFRESH)에서 받습니다. "
                    + "쿠키로 호출한 경우 응답에도 갱신된 access/refresh 쿠키를 Set-Cookie 합니다. "
                    + "매 호출 시 refresh token 도 회전(rotation)됩니다."
    )
    public ResponseEntity<ApiResponse<TokenPairResponse>> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        String cookieRefresh = AuthCookieReader.read(httpRequest, AuthCookies.REFRESH);
        String refreshToken = resolveRefreshToken(request, cookieRefresh);
        boolean fromCookie = cookieRefresh != null && cookieRefresh.equals(refreshToken);

        RefreshTokenService.TokenPair pair = refreshTokenService.rotate(refreshToken);
        TokenPairResponse body = new TokenPairResponse(
                pair.accessToken(),
                pair.refreshToken(),
                pair.accessExpiresInSeconds(),
                pair.refreshExpiresInSeconds()
        );

        if (!fromCookie) {
            return ResponseEntity.ok(ApiResponse.ok(body));
        }

        ResponseCookie access = socialAuthSupport.buildCookie(AuthCookies.ACCESS, pair.accessToken(), pair.accessExpiresInSeconds());
        ResponseCookie refresh = socialAuthSupport.buildCookie(AuthCookies.REFRESH, pair.refreshToken(), pair.refreshExpiresInSeconds());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, access.toString())
                .header(HttpHeaders.SET_COOKIE, refresh.toString())
                .body(ApiResponse.ok(body));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "로그아웃",
            description = "access token 을 블랙리스트에 등록하고 refresh token 을 revoke 합니다. "
                    + "Authorization 헤더 또는 BURTY_ACCESS 쿠키, refresh 는 body 또는 BURTY_REFRESH 쿠키를 사용합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        String accessToken = resolveAccessToken(authHeader, httpRequest);
        if (accessToken != null) {
            jwtBlacklistService.blacklist(accessToken);
        }

        String refreshToken = resolveRefreshToken(request, AuthCookieReader.read(httpRequest, AuthCookies.REFRESH));
        if (refreshToken != null) {
            refreshTokenService.revoke(refreshToken);
        }

        ResponseCookie expireAccess = socialAuthSupport.buildCookie(AuthCookies.ACCESS, "", 0);
        ResponseCookie expireRefresh = socialAuthSupport.buildCookie(AuthCookies.REFRESH, "", 0);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expireAccess.toString())
                .header(HttpHeaders.SET_COOKIE, expireRefresh.toString())
                .body(ApiResponse.ok(new LogoutResponse(true)));
    }

    private static String resolveRefreshToken(RefreshTokenRequest request, String cookieRefresh) {
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            return request.getRefreshToken().trim();
        }
        if (cookieRefresh != null && !cookieRefresh.isBlank()) {
            return cookieRefresh.trim();
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "refresh token 이 필요합니다.");
    }

    private static String resolveAccessToken(String authHeader, HttpServletRequest request) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return AuthCookieReader.read(request, AuthCookies.ACCESS);
    }
}
