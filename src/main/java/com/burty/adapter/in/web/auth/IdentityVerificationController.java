package com.burty.adapter.in.web.auth;

import com.burty.application.dto.identity.IdentityVerificationRequest;
import com.burty.application.dto.identity.IdentityVerificationResponse;
import com.burty.application.port.in.identity.IdentityVerificationUseCase;
import com.burty.core.annotation.CurrentUserId;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "BURTY Identity", description = "본인확인 API")
public class IdentityVerificationController extends BaseController {

  private final IdentityVerificationUseCase identityVerificationUseCase;

  @PostMapping("/identity/verify")
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "본인확인 요청", description = "NICE/KCB/Pass 스텁 연동으로 CI/DI를 발급합니다.")
  public ApiResponse<IdentityVerificationResponse> verify(
      @CurrentUserId String userId, @Valid @RequestBody IdentityVerificationRequest request) {
    var result =
        identityVerificationUseCase.verify(
            userId, request.name(), request.phone(), request.birthDate(), request.carrier());
    return ApiResponse.ok(
        new IdentityVerificationResponse(
            result.verified(), result.ci(), result.di(), result.message()));
  }
}
