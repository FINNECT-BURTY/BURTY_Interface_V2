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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/apple")
@Tag(name = "BURTY Auth - Apple", description = "Apple Sign in (authorize-url / SPA login / BFF callback). id_token 의 JWKS 서명 검증은 미구현.")
public class AppleAuthController extends BaseController {
    private static final SocialProvider PROVIDER = SocialProvider.APPLE;

    private final SocialLoginUseCase socialLoginUseCase;
    private final SocialAuthSupport support;

    public AppleAuthController(SocialLoginUseCase socialLoginUseCase, SocialAuthSupport support) {
        this.socialLoginUseCase = socialLoginUseCase;
        this.support = support;
    }

    @GetMapping("/authorize-url")
    @Operation(summary = "Apple 인가 URL 생성", description = "Apple Sign in 인가 URL과 state를 반환합니다. Apple 은 response_mode=form_post 사용. ")
    public ApiResponse<AuthorizeUrlResponse> authorizeUrl(@RequestParam(required = false) String state,
                                                          @RequestParam(required = false) String redirectUri) {
        SocialAuthorizeUrlResult auth = socialLoginUseCase.createAuthorizeUrl(PROVIDER.name(), state, redirectUri);
        return ApiResponse.ok(new AuthorizeUrlResponse(auth.authorizeUrl(), auth.state()));
    }

    @PostMapping("/login")
    @Operation(summary = "Apple 로그인 (SPA / API client 용)", description = "Apple authorization code를 검증하고 BURTY access + refresh token 쌍을 응답 body 로 반환합니다.")
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
    @Operation(summary = "Apple BFF 콜백", description = "Apple 이 redirect 하는 GET 엔드포인트. id_token 페이로드로 사용자 식별 후 access/refresh 쿠키 set + FE 로 302.")
    public ResponseEntity<Void> callback(@RequestParam(required = false) String code,
                                         @RequestParam(required = false) String state,
                                         @RequestParam(required = false) String error,
                                         @RequestParam(required = false, name = "error_description") String errorDescription) {
        return support.handleBffCallback(PROVIDER, code, state, error, errorDescription, socialLoginUseCase);
    }
}
