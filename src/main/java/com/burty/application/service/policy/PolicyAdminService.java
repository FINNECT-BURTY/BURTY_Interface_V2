/**
 *
 *
 * <pre>
 * <b>Description  : 정책 애플리케이션 서비스 (PolicyAdminService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.policy
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
package com.burty.application.service.policy;

import com.burty.adapter.in.web.mapper.WebResponseMapper;
import com.burty.application.dto.policy.PolicyAdminRequest;
import com.burty.application.dto.policy.PolicyAdminResponse;
import com.burty.application.port.in.policy.PolicyAdminUseCase;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.policy.entity.PolicyEntity;
import com.burty.domain.policy.repository.PolicyRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PolicyAdminService implements PolicyAdminUseCase {

  private final PolicyRepository policyRepository;
  private final WebResponseMapper webResponseMapper;

  @Override
  @Transactional(readOnly = true)
  public List<PolicyAdminResponse> listPolicies() {
    return policyRepository.findAll().stream().map(webResponseMapper::toResponse).toList();
  }

  @Override
  @Transactional
  public PolicyAdminResponse upsert(PolicyAdminRequest request) {
    PolicyEntity entity =
        policyRepository.findById(request.policyCode()).orElseGet(PolicyEntity::new);
    entity.setPolicyCode(request.policyCode());
    entity.setPolicyTypeCode(defaultString(request.policyTypeCode(), "FINANCE"));
    entity.setTitle(defaultString(request.title(), request.policyCode()));
    entity.setSupportType(request.supportType());
    entity.setAgeMin(request.ageMin());
    entity.setAgeMax(request.ageMax());
    entity.setIncomeMax(request.incomeMax());
    entity.setOccupationCode(request.occupationCode());
    entity.setResidenceCode(request.residenceCode());
    entity.setBenefitSummary(request.benefitSummary());
    entity.setApplyUrl(request.applyUrl());
    entity.setValidFrom(request.validFrom());
    entity.setValidTo(request.validTo());
    entity.setActive(request.active() == null || request.active());
    entity.setPriorityBase(request.priorityBase() == null ? 50 : request.priorityBase());
    return webResponseMapper.toResponse(policyRepository.save(entity));
  }

  @Override
  @Transactional
  public void deactivate(String policyCode) {
    PolicyEntity entity =
        policyRepository
            .findById(policyCode)
            .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "정책을 찾을 수 없습니다."));
    entity.setActive(false);
    policyRepository.save(entity);
  }

  private String defaultString(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
