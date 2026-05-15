package com.burty.adapter.in.web;

import com.burty.adapter.in.web.dto.PolicyAdminRequest;
import com.burty.adapter.in.web.dto.PolicyAdminResponse;
import com.burty.adapter.in.web.dto.SimpleResultResponse;
import com.burty.core.dto.response.ApiResponse;
import com.burty.domain.entity.PolicyEntity;
import com.burty.domain.repository.PolicyRepository;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/burty/admin/policies")
@Tag(name = "BURTY Policy Admin", description = "정책 데이터 최신화/관리자 API")
public class PolicyAdminController {
    private final PolicyRepository policyRepository;

    public PolicyAdminController(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @GetMapping
    @AuthLevel(RiskLevel.LEVEL_3)
    public ApiResponse<List<PolicyAdminResponse>> policies() {
        return ApiResponse.ok(policyRepository.findAll().stream().map(this::toResponse).toList());
    }

    @PostMapping
    @AuthLevel(RiskLevel.LEVEL_3)
    @Operation(summary = "정책 등록/수정", description = "정책명, 신청 URL, 지역/소득/나이 조건, 만료일을 등록 또는 수정합니다.")
    public ApiResponse<PolicyAdminResponse> upsert(@RequestBody PolicyAdminRequest request) {
        PolicyEntity entity = policyRepository.findById(request.getPolicyCode()).orElseGet(PolicyEntity::new);
        entity.setPolicyCode(request.getPolicyCode());
        entity.setPolicyTypeCode(defaultString(request.getPolicyTypeCode(), "FINANCE"));
        entity.setTitle(defaultString(request.getTitle(), request.getPolicyCode()));
        entity.setSupportType(request.getSupportType());
        entity.setAgeMin(request.getAgeMin());
        entity.setAgeMax(request.getAgeMax());
        entity.setIncomeMax(request.getIncomeMax());
        entity.setOccupationCode(request.getOccupationCode());
        entity.setResidenceCode(request.getResidenceCode());
        entity.setBenefitSummary(request.getBenefitSummary());
        entity.setApplyUrl(request.getApplyUrl());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
        entity.setActive(request.getActive() == null || request.getActive());
        entity.setPriorityBase(request.getPriorityBase() == null ? 50 : request.getPriorityBase());
        return ApiResponse.ok(toResponse(policyRepository.save(entity)));
    }

    @DeleteMapping("/{policyCode}")
    @AuthLevel(RiskLevel.LEVEL_3)
    public ApiResponse<SimpleResultResponse> deactivate(@PathVariable String policyCode) {
        PolicyEntity entity = policyRepository.findById(policyCode).orElseThrow();
        entity.setActive(false);
        policyRepository.save(entity);
        return ApiResponse.ok(new SimpleResultResponse(true, "정책이 비활성화되었습니다."));
    }

    private PolicyAdminResponse toResponse(PolicyEntity entity) {
        return new PolicyAdminResponse(
                entity.getPolicyCode(),
                entity.getPolicyTypeCode(),
                entity.getTitle(),
                entity.getApplyUrl(),
                entity.getValidTo(),
                Boolean.TRUE.equals(entity.getActive())
        );
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
