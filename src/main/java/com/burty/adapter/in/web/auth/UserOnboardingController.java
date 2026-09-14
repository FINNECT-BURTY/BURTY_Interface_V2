/**
 *
 *
 * <pre>
 * <b>Description  : 인증 API 컨트롤러 (UserOnboardingController)</b>
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

import com.burty.application.dto.auth.OnboardingProfileResponse;
import com.burty.application.dto.auth.ProfileOnboardingRequest;
import com.burty.application.dto.auth.SignupConsents;
import com.burty.application.port.in.auth.UserOnboardingUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.domain.auth.model.OnboardingProfileResult;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import com.burty.util.IpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/onboarding")
@Tag(name = "BURTY Onboarding", description = "소셜 로그인 이후 추가 프로필 등록")
@RequiredArgsConstructor
public class UserOnboardingController extends BaseController {

  private final UserOnboardingUseCase userOnboardingUseCase;

  @PostMapping("/profile")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(
      summary = "추가 프로필 등록",
      description = "휴대폰·실명·생년월일 등 추가 프로필 저장 (중복 시 alreadyRegistered).",
      security = {@SecurityRequirement(name = "bearerAuth")})
  public ApiResponse<OnboardingProfileResponse> completeProfile(
      @Valid @RequestBody ProfileOnboardingRequest request, HttpServletRequest httpRequest) {
    String userId =
        String.valueOf(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    // 동의 시점의 접속 정보를 함께 남긴다. X-Forwarded-For 를 그대로 믿으면 클라이언트가
    // 원하는 값이 기록되므로, 신뢰 프록시 판정을 거친 IpUtil 을 쓴다.
    OnboardingProfileResult result =
        userOnboardingUseCase.completeProfile(
            userId,
            request.phone(),
            request.name(),
            request.birthDate(),
            request.ageRange(),
            request.uxMode(),
            toConsents(request),
            IpUtil.getClientIp(httpRequest),
            httpRequest.getHeader("User-Agent"));
    return ApiResponse.ok(OnboardingProfileResponse.from(result));
  }

  /**
   * 요청의 동의 값.
   *
   * <p>항목별 값이 하나도 오지 않으면 옛 클라이언트다. FE 와 백엔드는 따로 배포되므로 그 동안 가입이 막히면 안 된다. 예전과 같이 필수 약관 두 건만 기록한다.
   */
  private static SignupConsents toConsents(ProfileOnboardingRequest request) {
    boolean noItemizedConsents =
        request.privacyAccepted() == null
            && request.creditCollectionAccepted() == null
            && request.creditTransferAccepted() == null
            && request.marketingAccepted() == null
            && request.benefitAccepted() == null
            && request.overseasTransferAccepted() == null;
    if (noItemizedConsents) {
      return SignupConsents.legacy(Boolean.TRUE.equals(request.termsAccepted()));
    }
    return new SignupConsents(
        Boolean.TRUE.equals(request.termsAccepted()),
        Boolean.TRUE.equals(request.privacyAccepted()),
        Boolean.TRUE.equals(request.creditCollectionAccepted()),
        Boolean.TRUE.equals(request.creditTransferAccepted()),
        Boolean.TRUE.equals(request.marketingAccepted()),
        Boolean.TRUE.equals(request.benefitAccepted()),
        Boolean.TRUE.equals(request.overseasTransferAccepted()));
  }
}
