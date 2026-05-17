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
import com.burty.security.AuthCookies;
import com.burty.security.JwtBlacklistService;
import com.burty.security.JwtTokenProvider;
import com.burty.security.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    public AuthController(JwtTokenProvider jwtTokenProvider, JwtBlacklistService jwtBlacklistService, RefreshTokenService refreshTokenService, BurtyAuthProperties burtyAuthProperties, Environment environment, SocialAuthSupport socialAuthSupport) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtBlacklistService = jwtBlacklistService;
        this.refreshTokenService = refreshTokenService;
        this.burtyAuthProperties = burtyAuthProperties;
        this.environment = environment;
        this.socialAuthSupport = socialAuthSupport;
    }

    @PostMapping("/refresh")
    @Operation(summary = "Access / Refresh token 재발급", description = "유효한 refresh token 으로 access + refresh 쌍을 재발급합니다. 매 호출 시 refresh token 도 회전(rotation)됩니다. 이미 revoke 된 refresh token 이 들어오면 도난 의심으로 해당 사용자의 모든 세션이 강제 종료됩니다.",)
    public ApiResponse<TokenPairResponse> refresh(@RequestBody RefreshTokenRequest request) {
        RefreshTokenService.TokenPair pair = refreshTokenService.rotate(request.getRefreshToken());
        return ApiResponse.ok(new TokenPairResponse(
                pair.accessToken(),
                pair.refreshToken(),
                pair.accessExpiresInSeconds(),
                pair.refreshExpiresInSeconds()
        ));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 access token 을 블랙리스트에 등록하고, body 로 받은 refresh token 을 revoke. 쿠키도 즉시 만료.", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                              @RequestBody(required = false) RefreshTokenRequest request) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwtBlacklistService.blacklist(authHeader.substring(7));
        }

        if (request != null) {
            refreshTokenService.revoke(request.getRefreshToken());
        }

        ResponseCookie expireAccess = socialAuthSupport.buildCookie(AuthCookies.ACCESS, "", 0);
        ResponseCookie expireRefresh = socialAuthSupport.buildCookie(AuthCookies.REFRESH, "", 0);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expireAccess.toString())
                .header(HttpHeaders.SET_COOKIE, expireRefresh.toString())
                .body(ApiResponse.ok(new LogoutResponse(true)));
    }
}
