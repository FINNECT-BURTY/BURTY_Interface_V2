/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 애플리케이션 서비스 (ConsentManagementService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.user
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
package com.burty.application.service.user;

import com.burty.application.dto.user.ConsentResponse;
import com.burty.application.port.in.user.ConsentManagementUseCase;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.auth.entity.BiometricCredentialEntity;
import com.burty.domain.auth.repository.BiometricCredentialRepository;
import com.burty.domain.auth.repository.SocialAccountRepository;
import com.burty.domain.user.entity.ConsentRecordEntity;
import com.burty.domain.user.repository.ConsentRecordRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsentManagementService implements ConsentManagementUseCase {

  private final ConsentRecordRepository consentRecordRepository;
  private final SocialAccountRepository socialAccountRepository;
  private final BiometricCredentialRepository biometricCredentialRepository;

  @Override
  @Transactional(readOnly = true)
  public List<ConsentResponse> listConsents(String userId) {
    Long userKey = Long.parseLong(userId);
    return consentRecordRepository.findByUser_UserIdOrderByAgreedAtDesc(userKey).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void revokeConsent(String userId, String consentId, String reason) {
    ConsentRecordEntity entity =
        consentRecordRepository
            .findById(Long.parseLong(consentId))
            .orElseThrow(
                () -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "동의 이력을 찾을 수 없습니다."));

    // 남의 동의는 철회할 수 없다. 예전에는 consentId 만으로 철회할 수 있었다.
    // 동의는 규제 기록이라 남의 것을 건드리면 그 사용자의 데이터 처리 근거가 사라진다.
    // 존재 여부를 알려주지 않도록 소유자가 아니면 "찾을 수 없음" 으로 답한다.
    if (!entity.getUser().getUserId().equals(Long.parseLong(userId))) {
      throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "동의 이력을 찾을 수 없습니다.");
    }

    entity.setRevokedAt(LocalDateTime.now());
    entity.setRevokeReason(reason == null ? "USER_REQUEST" : reason);
    consentRecordRepository.save(entity);
  }

  @Override
  @Transactional
  public void unlinkSocial(String userId, String provider) {
    socialAccountRepository
        .findByUserIdAndProvider(Long.parseLong(userId), provider.toUpperCase())
        .ifPresent(socialAccountRepository::delete);
  }

  @Override
  @Transactional
  public void revokeBiometric(String userId) {
    LocalDateTime now = LocalDateTime.now();
    for (BiometricCredentialEntity credential :
        biometricCredentialRepository.findByUser_UserIdAndRevokedAtIsNull(Long.parseLong(userId))) {
      credential.setRevokedAt(now);
      biometricCredentialRepository.save(credential);
    }
  }

  private ConsentResponse toResponse(ConsentRecordEntity entity) {
    return new ConsentResponse(
        entity.getConsentId().toString(),
        entity.getConsentType().name(),
        entity.getConsentVersion(),
        entity.getAgreedAt(),
        entity.getRevokedAt());
  }
}
