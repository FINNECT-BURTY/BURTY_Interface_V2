/**
 *
 *
 * <pre>
 * <b>Description  : 관리 애플리케이션 서비스 (AiTemplateAdminService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.admin
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
package com.burty.application.service.admin;

import com.burty.application.dto.admin.AiFallbackTemplateRequest;
import com.burty.application.dto.admin.AiFallbackTemplateResponse;
import com.burty.application.port.in.admin.AiTemplateAdminUseCase;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.admin.entity.AiFallbackTemplateEntity;
import com.burty.domain.admin.repository.AiFallbackTemplateRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiTemplateAdminService implements AiTemplateAdminUseCase {

  private final AiFallbackTemplateRepository repository;

  @Override
  @Transactional(readOnly = true)
  public List<AiFallbackTemplateResponse> listTemplates() {
    return repository.findAll().stream().map(AiFallbackTemplateResponse::from).toList();
  }

  @Override
  @Transactional
  public AiFallbackTemplateResponse upsert(AiFallbackTemplateRequest request) {
    AiFallbackTemplateEntity entity =
        repository.findById(request.templateKey()).orElseGet(AiFallbackTemplateEntity::new);
    entity.setTemplateKey(request.templateKey());
    entity.setRiskLevel(request.riskLevel());
    entity.setOccupationCode(request.occupationCode());
    entity.setCauseType(request.causeType());
    entity.setTemplateText(request.templateText());
    entity.setActive(request.active() == null || request.active());
    return AiFallbackTemplateResponse.from(repository.save(entity));
  }

  @Override
  @Transactional
  public void deactivate(String templateKey) {
    AiFallbackTemplateEntity entity =
        repository
            .findById(templateKey)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.ENTITY_NOT_FOUND, "AI fallback 템플릿을 찾을 수 없습니다."));
    entity.setActive(false);
    repository.save(entity);
  }
}
