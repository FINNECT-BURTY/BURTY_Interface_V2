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
import com.burty.security.JwtBlacklistService;
import com.burty.security.JwtTokenProvider;
import com.burty.security.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@Tag(name = "BURTY Auth", description = "BURTY 인증(JWT 발급/Refresh/로그아웃) API")
public class AuthController extends BaseController {
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtBlacklistService jwtBlacklistService;
    private final RefreshTokenService refreshTokenService;
    private final SocialLoginUseCase socialLoginUseCase;
    private final BurtyAuthProperties burtyAuthProperties;
    private final Environment environment;

    public AuthController(JwtTokenProvider jwtTokenProvider,
                          JwtBlacklistService jwtBlacklistService,
                          RefreshTokenService refreshTokenService,
                          SocialLoginUseCase socialLoginUseCase,
                          BurtyAuthProperties burtyAuthProperties,
                          Environment environment) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtBlacklistService = jwtBlacklistService;
        this.refreshTokenService = refreshTokenService;
        this.socialLoginUseCase = socialLoginUseCase;
        this.burtyAuthProperties = burtyAuthProperties;
        this.environment = environment;
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
            summary = "소셜 로그인",
            description = "GOOGLE/KAKAO/NAVER/APPLE authorization code를 검증하고 BURTY access + refresh token 쌍을 발급합니다.",
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

    @PostMapping("/logout")
    @Operation(
            summary = "로그아웃",
            description = "현재 access token 을 블랙리스트에 등록하고, body 로 받은 refresh token 을 revoke 합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    public ApiResponse<LogoutResponse> logout(@RequestHeader("Authorization") String authHeader,
                                              @RequestBody(required = false) RefreshTokenRequest request) {
        String token = authHeader.replace("Bearer ", "");
        jwtBlacklistService.blacklist(token);
        if (request != null) {
            refreshTokenService.revoke(request.getRefreshToken());
        }
        return ApiResponse.ok(new LogoutResponse(true));
    }
}
