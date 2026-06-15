/**
 *
 *
 * <pre>
 * <b>Description  : 가족보호 애플리케이션 서비스 (FamilyProtectionService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.family
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
package com.burty.application.service.family;

import com.burty.application.port.in.family.FamilyProtectionUseCase;
import com.burty.application.port.out.notify.FamilyAlertPort;
import com.burty.application.service.support.AuditLogger;
import com.burty.domain.family.entity.FamilyConsentEntity;
import com.burty.domain.family.model.FamilyAlert;
import com.burty.domain.family.model.FamilyConsent;
import com.burty.domain.family.model.FamilyDashboardSummary;
import com.burty.domain.family.repository.FamilyConsentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FamilyProtectionService implements FamilyProtectionUseCase {

  private final FamilyAlertPort familyAlertPort;
  private final AuditLogger auditLogger;
  private final FamilyConsentRepository familyConsentRepository;

  @Override
  public List<FamilyAlert> getFamilyAlerts(String userId) {
    return familyAlertPort.findByUserId(userId);
  }

  @Override
  public void registerFamilyConsent(String parentUserId, String childUserId) {
    FamilyConsentEntity entity =
        familyConsentRepository
            .findByParentUserIdAndChildUserId(parentUserId, childUserId)
            .orElseGet(FamilyConsentEntity::new);
    entity.setParentUserId(parentUserId);
    entity.setChildUserId(childUserId);
    entity.setConsented(true);
    familyConsentRepository.save(entity);
    auditLogger.logSuccess(parentUserId, "REGISTER_FAMILY_CONSENT", childUserId, "consent=true");
  }

  @Override
  public boolean updateFamilyConsent(String parentUserId, String childUserId, boolean consented) {
    return familyConsentRepository
        .findByParentUserIdAndChildUserId(parentUserId, childUserId)
        .map(
            entity -> {
              entity.setConsented(consented);
              familyConsentRepository.save(entity);
              return true;
            })
        .orElse(false);
  }

  @Override
  public boolean revokeFamilyConsent(String parentUserId, String childUserId) {
    return familyConsentRepository
        .findByParentUserIdAndChildUserId(parentUserId, childUserId)
        .map(
            entity -> {
              familyConsentRepository.delete(entity);
              return true;
            })
        .orElse(false);
  }

  @Override
  public List<FamilyConsent> getFamilyConsents(String parentUserId) {
    return familyConsentRepository.findByParentUserId(parentUserId).stream()
        .map(
            e ->
                new FamilyConsent(
                    e.getParentUserId(), e.getChildUserId(), Boolean.TRUE.equals(e.getConsented())))
        .toList();
  }

  @Override
  public FamilyDashboardSummary getFamilyDashboardSummary(String userId) {
    List<FamilyAlert> alerts = familyAlertPort.findByUserId(userId);
    int unusual = (int) alerts.stream().filter(it -> it.message().contains("이상거래")).count();
    return new FamilyDashboardSummary(userId, alerts.size(), unusual, 1);
  }
}
