/**
 *
 *
 * <pre>
 * <b>Description  : 인증 API 컨트롤러 (AuthController)</b>
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

import com.burty.application.dto.auth.CurrentUserResponse;
import com.burty.application.dto.auth.LogoutResponse;
import com.burty.application.dto.auth.RefreshTokenRequest;
import com.burty.application.dto.auth.TokenIssueRequest;
import com.burty.application.dto.auth.TokenPairMapper;
import com.burty.application.dto.auth.TokenPairResponse;
import com.burty.application.dto.auth.TokenResponse;
import com.burty.application.port.in.auth.AuthUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.security.AuthCookieFactory;
import com.burty.security.AuthCookieReader;
import com.burty.security.AuthCookies;
import com.burty.security.JwtBlacklistService;
import com.burty.security.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * BURTY 인증 기본 endpoint (JWT 발급/refresh/logout).
 *
 * <p>소셜 로그인은 {@link SocialAuthController} (/auth/{provider}/...) 에서 처리합니다.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "BURTY Auth", description = "BURTY 인증(JWT 발급/Refresh/로그아웃) API")
public class AuthController extends BaseController {

  private final AuthUseCase authUseCase;
  private final JwtBlacklistService jwtBlacklistService;
  private final RefreshTokenService refreshTokenService;
  private final AuthCookieFactory authCookieFactory;

  @PostMapping("/token")
  @Operation(
      summary = "JWT 발급 (테스트용)",
      description = "userId로 테스트 access token 발급 (test-token-enabled, non-prod 전용).",
      security = {})
  public ApiResponse<TokenResponse> issueToken(@RequestBody TokenIssueRequest request) {
    return ApiResponse.ok(new TokenResponse(authUseCase.issueTestToken(request.userId())));
  }

  @GetMapping("/me")
  @Operation(
      summary = "현재 로그인 사용자 조회",
      description = "HttpOnly access cookie 또는 Authorization 헤더로 현재 사용자와 추가 프로필 완료 여부를 조회합니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  public ApiResponse<CurrentUserResponse> me() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken
        || authentication.getPrincipal() == null
        || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
    }

    String userId = String.valueOf(authentication.getPrincipal());
    return ApiResponse.ok(authUseCase.currentUser(userId));
  }

  @PostMapping("/refresh")
  @Operation(
      summary = "Access / Refresh token 재발급",
      description = "refresh token(body/쿠키)으로 access·refresh 재발급 및 rotation.")
  public ResponseEntity<ApiResponse<TokenPairResponse>> refresh(
      @RequestBody(required = false) RefreshTokenRequest request, HttpServletRequest httpRequest) {
    String cookieRefresh = AuthCookieReader.read(httpRequest, AuthCookies.REFRESH);
    String refreshToken = resolveRefreshToken(request, cookieRefresh);
    boolean fromCookie = cookieRefresh != null && cookieRefresh.equals(refreshToken);

    RefreshTokenService.TokenPair pair = refreshTokenService.rotate(refreshToken);
    TokenPairResponse body = TokenPairMapper.toResponse(pair);

    if (!fromCookie) {
      return ResponseEntity.ok(ApiResponse.ok(body));
    }

    ResponseCookie access =
        authCookieFactory.sessionCookie(
            AuthCookies.ACCESS, pair.accessToken(), pair.accessExpiresInSeconds());
    ResponseCookie refresh =
        authCookieFactory.sessionCookie(
            AuthCookies.REFRESH, pair.refreshToken(), pair.refreshExpiresInSeconds());
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, access.toString())
        .header(HttpHeaders.SET_COOKIE, refresh.toString())
        .body(ApiResponse.ok(body));
  }

  @PostMapping("/logout")
  @Operation(
      summary = "로그아웃",
      description = "access 블랙리스트·refresh revoke (헤더 또는 BURTY_ACCESS/REFRESH 쿠키).",
      security = {@SecurityRequirement(name = "bearerAuth")})
  public ResponseEntity<ApiResponse<LogoutResponse>> logout(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestBody(required = false) RefreshTokenRequest request,
      HttpServletRequest httpRequest) {
    String accessToken = resolveAccessToken(authHeader, httpRequest);
    if (accessToken != null) {
      jwtBlacklistService.blacklist(accessToken);
    }

    String refreshToken =
        resolveRefreshToken(request, AuthCookieReader.read(httpRequest, AuthCookies.REFRESH));
    if (refreshToken != null) {
      refreshTokenService.revoke(refreshToken);
    }

    ResponseCookie expireAccess = authCookieFactory.expire(AuthCookies.ACCESS);
    ResponseCookie expireRefresh = authCookieFactory.expire(AuthCookies.REFRESH);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, expireAccess.toString())
        .header(HttpHeaders.SET_COOKIE, expireRefresh.toString())
        .body(ApiResponse.ok(new LogoutResponse(true)));
  }

  private static String resolveRefreshToken(RefreshTokenRequest request, String cookieRefresh) {
    if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
      return request.refreshToken().trim();
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
