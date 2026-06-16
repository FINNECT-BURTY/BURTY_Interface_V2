/**
 *
 *
 * <pre>
 * <b>Description  : 인증 API 컨트롤러 (SocialAuthController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.auth
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.adapter.in.web.auth;

import com.burty.adapter.in.web.social.OAuthFrontendRedirect;
import com.burty.adapter.in.web.social.SocialOAuthCallbackHandler;
import com.burty.application.dto.auth.AuthorizeUrlResponse;
import com.burty.application.dto.auth.SocialLoginRequest;
import com.burty.application.dto.auth.SocialLoginResponse;
import com.burty.application.port.in.auth.SocialLoginUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.domain.auth.model.SocialAuthorizeUrlResult;
import com.burty.domain.auth.model.SocialLoginResult;
import com.burty.domain.auth.model.SocialProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(
    name = "BURTY Auth - Social",
    description = "Kakao/Google/Naver/Apple OAuth (authorize, login, callback)")
public class SocialAuthController extends BaseController {

  private final SocialLoginUseCase socialLoginUseCase;
  private final OAuthFrontendRedirect frontendRedirect;
  private final SocialOAuthCallbackHandler callbackHandler;

  @GetMapping("/{provider}/authorize-url")
  @Operation(summary = "소셜 OAuth 인가 URL", description = "provider별 authorize URL 과 state 를 반환합니다.")
  public ApiResponse<AuthorizeUrlResponse> authorizeUrl(
      @PathVariable String provider,
      @RequestParam(required = false) String state,
      HttpServletRequest request) {
    SocialProvider socialProvider = SocialProvider.parse(provider);
    SocialAuthorizeUrlResult auth =
        socialLoginUseCase.createAuthorizeUrl(
            socialProvider.name(), state, frontendRedirect.resolveFromRequest(request));
    return ApiResponse.ok(new AuthorizeUrlResponse(auth.authorizeUrl(), auth.state()));
  }

  @PostMapping("/{provider}/login")
  @Operation(
      summary = "소셜 SPA 로그인",
      description = "authorization code 로 JWT 를 발급합니다.",
      security = {})
  public ApiResponse<SocialLoginResponse> login(
      @PathVariable String provider, @RequestBody SocialLoginRequest request) {
    SocialProvider socialProvider = SocialProvider.parse(provider);
    SocialLoginResult result =
        socialLoginUseCase.login(
            socialProvider.name(),
            request.code(),
            request.redirectUri(),
            request.state(),
            request.codeVerifier());
    return ApiResponse.ok(SocialLoginResponse.from(result));
  }

  @GetMapping("/{provider}/callback")
  @Operation(
      summary = "소셜 OAuth 콜백 (GET)",
      description = "BFF 콜백 — 프론트로 redirect 합니다.",
      security = {})
  public ResponseEntity<Void> callbackGet(
      @PathVariable String provider,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String error,
      @RequestParam(required = false, name = "error_description") String errorDescription) {
    return callbackHandler.handle(
        SocialProvider.parse(provider), code, state, error, errorDescription);
  }

  @PostMapping(
      value = "/{provider}/callback",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  @Operation(
      summary = "소셜 OAuth 콜백 (POST)",
      description = "Apple 등 form_post 콜백 처리.",
      security = {})
  public ResponseEntity<Void> callbackPost(
      @PathVariable String provider,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String error,
      @RequestParam(required = false, name = "error_description") String errorDescription) {
    return callbackHandler.handle(
        SocialProvider.parse(provider), code, state, error, errorDescription);
  }
}
