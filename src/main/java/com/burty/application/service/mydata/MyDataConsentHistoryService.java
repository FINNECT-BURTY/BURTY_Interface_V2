package com.burty.application.service.mydata;

import com.burty.config.BurtyOnboardingProperties;
import com.burty.domain.mydata.entity.MyDataConsentHistoryEntity;
import com.burty.domain.mydata.repository.MyDataConsentHistoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyDataConsentHistoryService {

  private final MyDataConsentHistoryRepository consentHistoryRepository;
  private final BurtyOnboardingProperties onboardingProperties;

  @Transactional
  public MyDataConsentHistoryEntity recordAgreement(
      String userId, String institutionCode, String scope, Long transmissionRequestId) {
    MyDataConsentHistoryEntity entity = new MyDataConsentHistoryEntity();
    entity.setUserId(userId);
    entity.setInstitutionCode(institutionCode);
    entity.setScope(scope);
    entity.setTransmissionRequestId(transmissionRequestId);
    entity.setConsentVersion(onboardingProperties.getTermsVersion());
    entity.setAgreedAt(LocalDateTime.now());
    return consentHistoryRepository.save(entity);
  }

  @Transactional
  public void revokeActiveConsents(String userId, String institutionCode, String reason) {
    List<MyDataConsentHistoryEntity> active =
        consentHistoryRepository.findByUserIdAndInstitutionCodeOrderByAgreedAtDesc(
            userId, institutionCode);
    LocalDateTime now = LocalDateTime.now();
    for (MyDataConsentHistoryEntity entity : active) {
      if (entity.getRevokedAt() == null) {
        entity.setRevokedAt(now);
        entity.setRevokeReason(reason);
        consentHistoryRepository.save(entity);
      }
    }
  }

  public List<MyDataConsentHistoryEntity> listByUser(String userId) {
    return consentHistoryRepository.findByUserIdOrderByAgreedAtDesc(userId);
  }
}
