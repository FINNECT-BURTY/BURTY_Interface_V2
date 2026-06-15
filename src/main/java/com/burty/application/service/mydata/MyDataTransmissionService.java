package com.burty.application.service.mydata;

import com.burty.application.port.in.mydata.MyDataTransmissionUseCase;
import com.burty.application.service.support.AuditLogger;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.mydata.entity.MyDataConsentHistoryEntity;
import com.burty.domain.mydata.entity.MyDataTransmissionLogEntity;
import com.burty.domain.mydata.entity.MyDataTransmissionRequestEntity;
import com.burty.domain.mydata.entity.MyDataTransmissionRequestEntity.Status;
import com.burty.domain.mydata.repository.MyDataTransmissionRequestRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyDataTransmissionService implements MyDataTransmissionUseCase {

  private final MyDataTransmissionRequestRepository requestRepository;
  private final MyDataConsentHistoryService consentHistoryService;
  private final MyDataTransmissionLogService transmissionLogService;
  private final MyDataAccountSyncService accountSyncService;
  private final AuditLogger auditLogger;

  @Override
  @Transactional
  public MyDataTransmissionRequestEntity createRequest(
      String userId, String institutionCode, String scope) {
    if (scope == null || scope.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "전송 범위(scope)는 필수입니다.");
    }
    MyDataTransmissionRequestEntity entity = new MyDataTransmissionRequestEntity();
    entity.setUserId(userId);
    entity.setInstitutionCode(institutionCode);
    entity.setScope(scope.trim());
    entity.setStatus(Status.REQUESTED);
    entity.setRequestedAt(LocalDateTime.now());
    entity.setConsentExpiresAt(LocalDateTime.now().plusYears(1));
    requestRepository.save(entity);
    transmissionLogService.logOutbound(
        userId, institutionCode, "TRANSMISSION_REQUEST", "scope=" + scope);
    auditLogger.logSuccess(
        userId, "MYDATA_TRANSMISSION_REQUEST", institutionCode, "scope=" + scope);
    return entity;
  }

  @Override
  public List<MyDataTransmissionRequestEntity> listRequests(String userId) {
    return requestRepository.findByUserIdOrderByRequestedAtDesc(userId);
  }

  @Override
  @Transactional
  public boolean revokeRequest(String userId, Long requestId, String reason) {
    return requestRepository
        .findById(requestId)
        .filter(r -> userId.equals(r.getUserId()))
        .map(
            entity -> {
              entity.setStatus(Status.REVOKED);
              entity.setRevokedAt(LocalDateTime.now());
              requestRepository.save(entity);
              consentHistoryService.revokeActiveConsents(
                  userId, entity.getInstitutionCode(), reason);
              transmissionLogService.logOutbound(
                  userId, entity.getInstitutionCode(), "TRANSMISSION_REVOKE", reason);
              auditLogger.logSuccess(
                  userId, "MYDATA_TRANSMISSION_REVOKE", entity.getInstitutionCode(), reason);
              return true;
            })
        .orElse(false);
  }

  @Override
  public List<MyDataConsentHistoryEntity> listConsents(String userId) {
    return consentHistoryService.listByUser(userId);
  }

  @Override
  public List<MyDataTransmissionLogEntity> listTransmissionLogs(String userId, int days) {
    return transmissionLogService.listForUser(userId, days);
  }

  @Override
  @Transactional
  public int syncAccounts(String userId) {
    return accountSyncService.syncFromOpenBanking(userId);
  }

  /** OAuth 성공 후 전송요구 활성화 및 동의 기록. 사전 요청이 없으면 동의만 기록. */
  @Transactional
  public void activateAfterOAuth(String userId, String institutionCode, String scope) {
    var pending =
        requestRepository.findFirstByUserIdAndInstitutionCodeAndStatusOrderByRequestedAtDesc(
            userId, institutionCode, Status.REQUESTED);
    if (pending.isPresent()) {
      MyDataTransmissionRequestEntity entity = pending.get();
      entity.setStatus(Status.ACTIVE);
      entity.setAuthorizedAt(LocalDateTime.now());
      requestRepository.save(entity);
      consentHistoryService.recordAgreement(userId, institutionCode, scope, entity.getRequestId());
    } else {
      consentHistoryService.recordAgreement(userId, institutionCode, scope, null);
    }
    transmissionLogService.logInbound(
        userId, institutionCode, "OAUTH_SUCCESS", "institution linked");
  }
}
