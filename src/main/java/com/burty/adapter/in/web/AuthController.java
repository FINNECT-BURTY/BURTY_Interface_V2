package com.burty.adapter.in.web;

import com.burty.adapter.in.web.dto.SocialLoginRequest;
import com.burty.adapter.in.web.dto.SocialLoginResponse;
import com.burty.adapter.in.web.dto.TokenIssueRequest;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@Tag(name = "BURTY Auth", description = "BURTY 인증(JWT 발급/로그아웃) API")
public class AuthController extends BaseController {
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtBlacklistService jwtBlacklistService;
    private final SocialLoginUseCase socialLoginUseCase;
    private final BurtyAuthProperties burtyAuthProperties;
    private final Environment environment;

    public AuthController(JwtTokenProvider jwtTokenProvider,
                          JwtBlacklistService jwtBlacklistService,
                          SocialLoginUseCase socialLoginUseCase,
                          BurtyAuthProperties burtyAuthProperties,
                          Environment environment) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtBlacklistService = jwtBlacklistService;
        this.socialLoginUseCase = socialLoginUseCase;
        this.burtyAuthProperties = burtyAuthProperties;
        this.environment = environment;
    }

    @PostMapping("/token")
    @Operation(
            summary = "JWT 발급",
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

    @GetMapping("/social/{provider}/authorize-url")
    @Operation(
            summary = "소셜 로그인 인가 URL 생성",
            description = "GOOGLE/KAKAO/NAVER/APPLE OAuth 인가 URL과 state를 반환합니다. `burty.social.stub-mode=false`일 때 로그인 요청의 state와 일치해야 합니다.",
            security = {}
    )
    public ApiResponse<AuthorizeUrlResponse> socialAuthorizeUrl(@PathVariable String provider,
                                                                @RequestParam(required = false) String state) {
        var auth = socialLoginUseCase.createAuthorizeUrl(provider, state);
        return ApiResponse.ok(new AuthorizeUrlResponse(auth.authorizeUrl(), auth.state()));
    }

    @PostMapping("/social/{provider}/login")
    @Operation(
            summary = "소셜 로그인",
            description = "GOOGLE/KAKAO/NAVER/APPLE authorization code를 검증하고 BURTY JWT를 발급합니다.",
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
                result.isNewUser(),
                result.isProfileComplete()
        ));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "로그아웃",
            description = "현재 Bearer 토큰을 블랙리스트에 등록합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    public ApiResponse<LogoutResponse> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        jwtBlacklistService.blacklist(token);
        return ApiResponse.ok(new LogoutResponse(true));
    }
}
