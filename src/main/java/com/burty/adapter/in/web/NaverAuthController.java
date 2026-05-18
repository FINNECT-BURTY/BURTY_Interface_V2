package com.burty.adapter.in.web;

import com.burty.adapter.in.web.dto.AuthorizeUrlResponse;
import com.burty.adapter.in.web.dto.SocialLoginRequest;
import com.burty.adapter.in.web.dto.SocialLoginResponse;
import com.burty.adapter.in.web.support.SocialAuthSupport;
import com.burty.application.port.in.SocialLoginUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.domain.model.SocialAuthorizeUrlResult;
import com.burty.domain.model.SocialLoginResult;
import com.burty.domain.model.SocialProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/naver")
@Tag(name = "BURTY Auth - Naver", description = "네이버 OAuth 로그인 (authorize-url / SPA login / BFF callback)")
public class NaverAuthController extends BaseController {
    private static final SocialProvider PROVIDER = SocialProvider.NAVER;

    private final SocialLoginUseCase socialLoginUseCase;
    private final SocialAuthSupport support;

    public NaverAuthController(SocialLoginUseCase socialLoginUseCase, SocialAuthSupport support) {
        this.socialLoginUseCase = socialLoginUseCase;
        this.support = support;
    }

    @GetMapping("/authorize-url")
    @Operation(
            summary = "네이버 인가 URL 생성",
            description = "네이버 OAuth 인가 URL과 state를 반환합니다.",
            security = {}
    )
    public ApiResponse<AuthorizeUrlResponse> authorizeUrl(@RequestParam(required = false) String state,
                                                          HttpServletRequest request) {
        SocialAuthorizeUrlResult auth = socialLoginUseCase.createAuthorizeUrl(
                PROVIDER.name(),
                state,
                support.resolveRequestFrontendOrigin(request)
        );
        return ApiResponse.ok(new AuthorizeUrlResponse(auth.authorizeUrl(), auth.state()));
    }

    @PostMapping("/login")
    @Operation(
            summary = "네이버 로그인 (SPA / API client 용)",
            description = "네이버 authorization code를 검증하고 BURTY access + refresh token 쌍을 응답 body 로 반환합니다.",
            security = {}
    )
    public ApiResponse<SocialLoginResponse> login(@RequestBody SocialLoginRequest request) {
        SocialLoginResult result = socialLoginUseCase.login(
                PROVIDER.name(),
                request.getCode(),
                request.getRedirectUri(),
                request.getState(),
                request.getCodeVerifier()
        );
        return ApiResponse.ok(support.toResponse(result));
    }

    @GetMapping("/callback")
    @Operation(
            summary = "네이버 BFF 콜백",
            description = "네이버가 redirect 하는 GET 엔드포인트. 토큰 교환 후 access/refresh 를 HttpOnly 쿠키로 set + FE 페이지로 302.",
            security = {}
    )
    public ResponseEntity<Void> callback(@RequestParam(required = false) String code,
                                         @RequestParam(required = false) String state,
                                         @RequestParam(required = false) String error,
                                         @RequestParam(required = false, name = "error_description") String errorDescription) {
        return support.handleBffCallback(PROVIDER, code, state, error, errorDescription, socialLoginUseCase);
    }
}
