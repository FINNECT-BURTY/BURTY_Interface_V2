package com.burty.adapter.in.web;

import com.burty.core.controller.BaseController;

import com.burty.adapter.in.web.dto.ConsentResponse;
import com.burty.adapter.in.web.dto.SimpleResultResponse;
import com.burty.core.dto.response.ApiResponse;
import com.burty.domain.entity.BiometricCredentialEntity;
import com.burty.domain.entity.ConsentRecordEntity;
import com.burty.domain.entity.MyDataLinkStatusEntity;
import com.burty.domain.entity.SocialAccountEntity;
import com.burty.domain.repository.BiometricCredentialRepository;
import com.burty.domain.repository.ConsentRecordRepository;
import com.burty.domain.repository.MyDataLinkStatusRepository;
import com.burty.domain.repository.SocialAccountRepository;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/consents")
@Tag(name = "BURTY Consent Management", description = "동의/연결 해제 관리 API")
public class ConsentManagementController extends BaseController {
    private final ConsentRecordRepository consentRecordRepository;
    private final MyDataLinkStatusRepository myDataLinkStatusRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final BiometricCredentialRepository biometricCredentialRepository;

    public ConsentManagementController(ConsentRecordRepository consentRecordRepository,
                                       MyDataLinkStatusRepository myDataLinkStatusRepository,
                                       SocialAccountRepository socialAccountRepository,
                                       BiometricCredentialRepository biometricCredentialRepository) {
        this.consentRecordRepository = consentRecordRepository;
        this.myDataLinkStatusRepository = myDataLinkStatusRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.biometricCredentialRepository = biometricCredentialRepository;
    }

    @GetMapping
    @AuthLevel(RiskLevel.LEVEL_2)
    @Operation(summary = "동의 이력 조회", description = "사용자의 개인정보/마이데이터/위치/보안 로그 동의 이력을 조회합니다.")
    public ApiResponse<List<ConsentResponse>> consents(@RequestParam String userId) {
        Long userKey = Long.parseLong(userId);
        return ApiResponse.ok(consentRecordRepository.findByUser_UserIdOrderByAgreedAtDesc(userKey).stream()
                .map(c -> new ConsentResponse(
                        c.getConsentId().toString(),
                        c.getConsentType().name(),
                        c.getConsentVersion(),
                        c.getAgreedAt(),
                        c.getRevokedAt()
                ))
                .toList());
    }

    @PostMapping("/{consentId}/revoke")
    @AuthLevel(RiskLevel.LEVEL_3)
    @Operation(summary = "동의 철회", description = "동의 이력을 철회 처리합니다.")
    public ApiResponse<SimpleResultResponse> revokeConsent(@PathVariable String consentId,
                                                           @RequestParam(required = false) String reason) {
        ConsentRecordEntity entity = consentRecordRepository.findById(Long.parseLong(consentId)).orElseThrow();
        entity.setRevokedAt(LocalDateTime.now());
        entity.setRevokeReason(reason == null ? "USER_REQUEST" : reason);
        consentRecordRepository.save(entity);
        return ApiResponse.ok(new SimpleResultResponse(true, "동의가 철회되었습니다."));
    }

    @DeleteMapping("/mydata/{institutionCode}")
    @AuthLevel(RiskLevel.LEVEL_3)
    @Operation(summary = "마이데이터 연결 해제", description = "특정 기관의 마이데이터 연결을 해제합니다.")
    public ApiResponse<SimpleResultResponse> unlinkMyData(@RequestParam String userId, @PathVariable String institutionCode) {
        myDataLinkStatusRepository.findByUserIdAndInstitutionCode(userId, institutionCode).ifPresent(entity -> {
            entity.setStatus("UNLINKED");
            entity.setUnlinkedAt(LocalDateTime.now());
            myDataLinkStatusRepository.save(entity);
        });
        return ApiResponse.ok(new SimpleResultResponse(true, "마이데이터 연결이 해제되었습니다."));
    }

    @DeleteMapping("/social/{provider}")
    @AuthLevel(RiskLevel.LEVEL_3)
    @Operation(summary = "소셜 로그인 연결 해제", description = "카카오/네이버/애플 소셜 계정 연결을 해제합니다.")
    public ApiResponse<SimpleResultResponse> unlinkSocial(@RequestParam String userId, @PathVariable String provider) {
        socialAccountRepository.findByUserIdAndProvider(Long.parseLong(userId), provider.toUpperCase())
                .ifPresent(socialAccountRepository::delete);
        return ApiResponse.ok(new SimpleResultResponse(true, "소셜 로그인 연결이 해제되었습니다."));
    }

    @DeleteMapping("/biometric")
    @AuthLevel(RiskLevel.LEVEL_3)
    @Operation(summary = "생체 인증 해제", description = "사용자의 모든 활성 생체 credential을 폐기합니다.")
    public ApiResponse<SimpleResultResponse> revokeBiometric(@RequestParam String userId) {
        LocalDateTime now = LocalDateTime.now();
        for (BiometricCredentialEntity credential : biometricCredentialRepository.findByUser_UserIdAndRevokedAtIsNull(Long.parseLong(userId))) {
            credential.setRevokedAt(now);
            biometricCredentialRepository.save(credential);
        }
        return ApiResponse.ok(new SimpleResultResponse(true, "생체 인증이 해제되었습니다."));
    }
}
