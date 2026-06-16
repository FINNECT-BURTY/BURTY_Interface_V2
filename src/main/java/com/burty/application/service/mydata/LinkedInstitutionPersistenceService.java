package com.burty.application.service.mydata;

import com.burty.domain.mydata.entity.LinkedInstitutionEntity;
import com.burty.domain.mydata.entity.LinkedInstitutionEntity.InstitutionType;
import com.burty.domain.mydata.entity.LinkedInstitutionEntity.LinkStatus;
import com.burty.domain.mydata.model.MyDataTokenBundle;
import com.burty.domain.mydata.repository.LinkedInstitutionRepository;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.UserRepository;
import com.burty.util.FieldEncryptor;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** LinkedInstitution 토큰 DB 영구 저장 (암호화). */
@Service
@RequiredArgsConstructor
public class LinkedInstitutionPersistenceService {

  public static final String OPEN_BANKING_CODE = "OPENBANKING";

  private static final Map<String, String> INSTITUTION_NAMES =
      Map.of(
          "MYDATA", "마이데이터 통합",
          "OPENBANKING", "오픈뱅킹",
          "004", "KB국민은행",
          "088", "신한은행",
          "090", "카카오뱅크",
          "081", "하나은행",
          "011", "NH농협은행");

  private final LinkedInstitutionRepository linkedInstitutionRepository;
  private final UserRepository userRepository;
  private final FieldEncryptor fieldEncryptor;

  @Transactional
  public LinkedInstitutionEntity saveTokens(
      String userId, String institutionCode, MyDataTokenBundle bundle) {
    if (bundle == null || bundle.accessToken() == null || bundle.accessToken().isBlank()) {
      throw new IllegalArgumentException("유효한 access token이 필요합니다.");
    }
    Long numericUserId = Long.parseLong(userId);
    UserEntity user =
        userRepository
            .findById(numericUserId)
            .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다: " + userId));

    LinkedInstitutionEntity entity =
        linkedInstitutionRepository
            .findByUser_UserIdAndInstitutionCode(numericUserId, institutionCode)
            .orElseGet(LinkedInstitutionEntity::new);

    entity.setUser(user);
    entity.setInstitutionCode(institutionCode);
    entity.setInstitutionName(INSTITUTION_NAMES.getOrDefault(institutionCode, institutionCode));
    entity.setInstitutionType(InstitutionType.BANK);
    entity.setAccessToken(fieldEncryptor.encrypt(bundle.accessToken()));
    if (bundle.refreshToken() != null && !bundle.refreshToken().isBlank()) {
      entity.setRefreshToken(fieldEncryptor.encrypt(bundle.refreshToken()));
    }
    entity.setTokenExpiresAt(
        bundle.tokenExpiresAt() != null
            ? bundle.tokenExpiresAt()
            : LocalDateTime.now().plusHours(1));
    entity.setConsentExpiresAt(LocalDateTime.now().plusYears(1));
    entity.setStatus(LinkStatus.ACTIVE);
    entity.setLastSyncedAt(LocalDateTime.now());
    entity.setLastErrorCode(null);
    entity.setLastErrorAt(null);
    return linkedInstitutionRepository.save(entity);
  }

  public Optional<MyDataTokenBundle> loadTokenBundle(String userId, String institutionCode) {
    return linkedInstitutionRepository
        .findByUser_UserIdAndInstitutionCode(Long.parseLong(userId), institutionCode)
        .filter(link -> link.getStatus() == LinkStatus.ACTIVE)
        .map(
            link -> {
              String access = fieldEncryptor.decrypt(link.getAccessToken());
              if (access == null || access.isBlank()) {
                return null;
              }
              String refresh =
                  link.getRefreshToken() == null
                      ? null
                      : fieldEncryptor.decrypt(link.getRefreshToken());
              return new MyDataTokenBundle(access, refresh, link.getTokenExpiresAt());
            })
        .filter(bundle -> bundle != null);
  }

  @Transactional
  public void markRevoked(String userId, String institutionCode) {
    linkedInstitutionRepository
        .findByUser_UserIdAndInstitutionCode(Long.parseLong(userId), institutionCode)
        .ifPresent(
            entity -> {
              entity.setStatus(LinkStatus.REVOKED);
              entity.setAccessToken(fieldEncryptor.encrypt("REVOKED"));
              entity.setRefreshToken(fieldEncryptor.encrypt("REVOKED"));
              entity.setLastErrorAt(LocalDateTime.now());
              linkedInstitutionRepository.save(entity);
            });
  }
}
