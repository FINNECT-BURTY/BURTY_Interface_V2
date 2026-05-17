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
@RequestMapping("/auth/kakao")
@Tag(name = "BURTY Auth - Kakao", description = "카카오 OAuth 로그인 (authorize-url / SPA login / BFF callback)")
public class KakaoAuthController extends BaseController {
    private static final SocialProvider PROVIDER = SocialProvider.KAKAO;

    private final SocialLoginUseCase socialLoginUseCase;
    private final SocialAuthSupport support;

    public KakaoAuthController(SocialLoginUseCase socialLoginUseCase, SocialAuthSupport support) {
        this.socialLoginUseCase = socialLoginUseCase;
        this.support = support;
    }

    @GetMapping("/authorize-url")
    @Operation(
            summary = "카카오 인가 URL 생성",
            description = "카카오 OAuth 인가 URL과 state를 반환합니다. `redirectUri` 를 query param 으로 넘기면 그 값을 카카오에 전달 " +
                    "— 단 해당 URI 가 카카오 콘솔에 사전 등록되어 있어야 함 (등록 안 되면 KOE006). " +
                    "넘기지 않으면 properties 의 `burty.social.kakao.redirect-uri` default 사용. " +
                    "`burty.social.stub-mode=false` 일 때 로그인 요청의 state 와 redirectUri 는 여기서 받은 값을 그대로 다시 보내야 함.",
            security = {}
    )
    public ApiResponse<AuthorizeUrlResponse> authorizeUrl(@RequestParam(required = false) String state,
                                                          @RequestParam(required = false) String redirectUri) {
        SocialAuthorizeUrlResult auth = socialLoginUseCase.createAuthorizeUrl(PROVIDER.name(), state, redirectUri);
        return ApiResponse.ok(new AuthorizeUrlResponse(auth.authorizeUrl(), auth.state()));
    }

    @PostMapping("/login")
    @Operation(
            summary = "카카오 로그인 (SPA / API client 용)",
            description = "카카오 authorization code를 검증하고 BURTY access + refresh token 쌍을 응답 body 로 반환합니다. " +
                    "SPA 가 FE 콜백 페이지에서 code 를 수신해 호출하는 패턴.",
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
            summary = "카카오 BFF 콜백",
            description = "카카오가 redirect 하는 GET 엔드포인트. 토큰 교환 후 access/refresh 를 HttpOnly 쿠키로 set + FE 페이지로 302.",
            security = {}
    )
    public ResponseEntity<Void> callback(@RequestParam(required = false) String code,
                                         @RequestParam(required = false) String state,
                                         @RequestParam(required = false) String error,
                                         @RequestParam(required = false, name = "error_description") String errorDescription) {
        return support.handleBffCallback(PROVIDER, code, state, error, errorDescription, socialLoginUseCase);
    }
}
