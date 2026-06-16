/**
 *
 *
 * <pre>
 * <b>Description  : 소셜로그인 핸들러 (SocialOAuthCallbackHandler)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.social
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
package com.burty.adapter.in.web.social;

import com.burty.application.port.in.auth.SocialLoginUseCase;
import com.burty.core.constant.LogMessages;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.auth.model.SocialLoginResult;
import com.burty.domain.auth.model.SocialProvider;
import com.burty.security.AuthCookieFactory;
import com.burty.security.AuthCookies;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SocialOAuthCallbackHandler {

  private static final Logger log = LoggerFactory.getLogger(SocialOAuthCallbackHandler.class);

  private final SocialLoginUseCase socialLoginUseCase;
  private final AuthCookieFactory authCookieFactory;
  private final OAuthFrontendRedirect frontendRedirect;

  public ResponseEntity<Void> handle(
      SocialProvider provider, String code, String state, String error, String errorDescription) {
    log.info(
        LogMessages.Auth.OAUTH_CALLBACK,
        provider,
        notBlank(code),
        notBlank(state),
        notBlank(error));

    if (notBlank(error)) {
      log.warn(
          "OAuth provider error provider={} error={} description={}",
          provider,
          error,
          errorDescription);
      return redirectError(mapProviderError(error));
    }
    if (!notBlank(code)) {
      log.warn("OAuth callback missing code provider={}", provider);
      return redirectError("missing_code");
    }

    try {
      SocialLoginResult result = socialLoginUseCase.login(provider.name(), code, null, state, null);
      ResponseCookie access =
          authCookieFactory.sessionCookie(
              AuthCookies.ACCESS, result.accessToken(), result.accessExpiresInSeconds());
      ResponseCookie refresh =
          authCookieFactory.sessionCookie(
              AuthCookies.REFRESH, result.refreshToken(), result.refreshExpiresInSeconds());
      String location =
          frontendRedirect.successUrl(
              result.frontendOrigin(), result.newUser(), result.profileComplete());
      log.info(
          LogMessages.Auth.OAUTH_SUCCESS,
          provider,
          result.userId(),
          result.newUser(),
          result.profileComplete());
      return ResponseEntity.status(HttpStatus.FOUND)
          .header(HttpHeaders.SET_COOKIE, access.toString())
          .header(HttpHeaders.SET_COOKIE, refresh.toString())
          .location(URI.create(location))
          .build();
    } catch (BusinessException businessException) {
      String errorCode = mapBusinessError(businessException);
      log.warn(
          "OAuth business error provider={} code={} reason={}",
          provider,
          errorCode,
          businessException.getMessage());
      return redirectError(errorCode);
    } catch (Exception unexpected) {
      log.error("OAuth unexpected error provider={}", provider, unexpected);
      return redirectError("internal_error");
    }
  }

  private ResponseEntity<Void> redirectError(String errorCode) {
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(frontendRedirect.safeErrorUrl(errorCode)))
        .build();
  }

  private static String mapProviderError(String providerError) {
    if (providerError == null) {
      return "internal_error";
    }
    String normalized = providerError.toLowerCase();
    if (normalized.contains("access_denied") || normalized.contains("user_cancelled")) {
      return "user_cancelled";
    }
    if (normalized.contains("invalid_request") || normalized.contains("invalid_redirect")) {
      return "invalid_request";
    }
    if (normalized.contains("server_error") || normalized.contains("temporarily_unavailable")) {
      return "provider_unavailable";
    }
    return "provider_error";
  }

  private static String mapBusinessError(BusinessException exception) {
    String message = exception.getMessage() == null ? "" : exception.getMessage();
    ErrorCode code = exception.getErrorCode();
    if (code == ErrorCode.INVALID_INPUT_VALUE) {
      if (message.contains("state")) {
        return "state_expired";
      }
      if (message.contains("code")) {
        return "invalid_code";
      }
      if (message.contains("provider") || message.contains("지원")) {
        return "unsupported_provider";
      }
      return "invalid_request";
    }
    if (code == ErrorCode.EXTERNAL_API_ERROR) {
      return "provider_error";
    }
    if (code == ErrorCode.INVALID_TOKEN || code == ErrorCode.EXPIRED_TOKEN) {
      return "invalid_token";
    }
    if (code == ErrorCode.FORBIDDEN) {
      return "forbidden";
    }
    return "internal_error";
  }

  private static boolean notBlank(String value) {
    return value != null && !value.isBlank();
  }
}
