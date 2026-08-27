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
import com.burty.application.port.in.auth.UserOnboardingUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.domain.auth.model.OnboardingProfileResult;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
      @Valid @RequestBody ProfileOnboardingRequest request) {
    String userId =
        String.valueOf(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    boolean terms = Boolean.TRUE.equals(request.termsAccepted());
    OnboardingProfileResult result =
        userOnboardingUseCase.completeProfile(
            userId,
            request.phone(),
            request.name(),
            request.birthDate(),
            request.ageRange(),
            request.uxMode(),
            terms);
    return ApiResponse.ok(OnboardingProfileResponse.from(result));
  }
}
