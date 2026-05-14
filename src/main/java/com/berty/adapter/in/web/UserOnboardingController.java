package com.berty.adapter.in.web;

import com.berty.adapter.in.web.dto.OnboardingProfileResponse;
import com.berty.adapter.in.web.dto.ProfileOnboardingRequest;
import com.berty.application.port.in.UserOnboardingUseCase;
import com.berty.core.dto.response.ApiResponse;
import com.berty.domain.model.OnboardingProfileResult;
import com.berty.security.AuthLevel;
import com.berty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/berty/onboarding")
@Tag(name = "BERTY Onboarding", description = "소셜 로그인 이후 추가 프로필 등록")
public class UserOnboardingController {

    private final UserOnboardingUseCase userOnboardingUseCase;

    public UserOnboardingController(UserOnboardingUseCase userOnboardingUseCase) {
        this.userOnboardingUseCase = userOnboardingUseCase;
    }

    @PostMapping("/profile")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(
            summary = "추가 프로필 등록",
            description = "JWT subject와 동일한 사용자에 대해 휴대폰·실명·생년월일 등을 저장합니다. 이미 등록된 경우 alreadyRegistered=true로 응답합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    public ApiResponse<OnboardingProfileResponse> completeProfile(@RequestBody ProfileOnboardingRequest request) {
        String userId = String.valueOf(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        boolean terms = Boolean.TRUE.equals(request.getTermsAccepted());
        OnboardingProfileResult result = userOnboardingUseCase.completeProfile(
                userId,
                request.getPhone(),
                request.getName(),
                request.getBirthDate(),
                request.getAgeRange(),
                request.getUxMode(),
                terms
        );
        return ApiResponse.ok(new OnboardingProfileResponse(result.completed(), result.alreadyRegistered()));
    }
}
