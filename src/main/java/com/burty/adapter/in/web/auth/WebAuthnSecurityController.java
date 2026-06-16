/**
 *
 *
 * <pre>
 * <b>Description  : 인증 API 컨트롤러 (WebAuthnSecurityController)</b>
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

import com.burty.adapter.in.web.mapper.WebResponseMapper;
import com.burty.application.dto.auth.BiometricAuthResponse;
import com.burty.application.dto.auth.ChallengeResponse;
import com.burty.application.dto.auth.RiskProofResponse;
import com.burty.application.dto.auth.WebAuthnBeginRequest;
import com.burty.application.dto.auth.WebAuthnFinishRequest;
import com.burty.application.port.in.auth.WebAuthnUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.domain.auth.model.BiometricAuthResult;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import com.burty.security.RiskProofService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "BURTY WebAuthn", description = "WebAuthn 생체 인증 API")
public class WebAuthnSecurityController extends BaseController {

  private final WebAuthnUseCase webAuthnUseCase;
  private final RiskProofService riskProofService;
  private final WebResponseMapper webResponseMapper;

  @PostMapping("/security/level2/proof")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<RiskProofResponse> issueLevel2Proof() {
    String userId =
        String.valueOf(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    return ApiResponse.ok(new RiskProofResponse(riskProofService.issue(userId, RiskLevel.LEVEL_2)));
  }

  @PostMapping("/security/webauthn/register/begin")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<ChallengeResponse> beginRegistration(
      @RequestBody WebAuthnBeginRequest request) {
    return ApiResponse.ok(
        new ChallengeResponse(webAuthnUseCase.beginRegistration(request.userId())));
  }

  @PostMapping("/security/webauthn/register/finish")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<BiometricAuthResponse> finishRegistration(
      @RequestBody WebAuthnFinishRequest request) {
    BiometricAuthResult result =
        webAuthnUseCase.finishRegistration(
            request.userId(),
            request.challengeId(),
            request.payload(),
            request.deviceFingerprint(),
            request.platform(),
            request.biometricType());
    return ApiResponse.ok(toBiometricResponse(request.userId(), result, true));
  }

  @PostMapping("/security/webauthn/authenticate/begin")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<ChallengeResponse> beginAuthentication(
      @RequestBody WebAuthnBeginRequest request) {
    return ApiResponse.ok(
        new ChallengeResponse(webAuthnUseCase.beginAuthentication(request.userId())));
  }

  @PostMapping("/security/webauthn/authenticate/finish")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<BiometricAuthResponse> finishAuthentication(
      @RequestBody WebAuthnFinishRequest request) {
    BiometricAuthResult result =
        webAuthnUseCase.finishAuthentication(
            request.userId(), request.challengeId(), request.payload(), request.deviceToken());
    return ApiResponse.ok(toBiometricResponse(request.userId(), result, false));
  }

  private BiometricAuthResponse toBiometricResponse(
      String userId, BiometricAuthResult result, boolean includeDeviceToken) {
    String riskProof =
        result.authenticated() ? riskProofService.issue(userId, RiskLevel.LEVEL_3) : null;
    return webResponseMapper.toBiometricResponse(result, riskProof, includeDeviceToken);
  }
}
