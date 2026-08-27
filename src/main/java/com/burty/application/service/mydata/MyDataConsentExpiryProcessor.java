package com.burty.application.service.mydata;

import com.burty.application.port.out.notify.FamilyAlertPort;
import com.burty.domain.mydata.entity.MyDataTransmissionRequestEntity;
import com.burty.domain.mydata.entity.MyDataTransmissionRequestEntity.Status;
import com.burty.domain.mydata.repository.MyDataTransmissionRequestRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 동의 만료 처리의 트랜잭션 단위.
 *
 * <p>배치 클래스 안에 두면 자기 호출이라 {@code @Transactional} 프록시가 적용되지 않는다. 한 건씩 독립 트랜잭션으로 처리해야 한 건의 실패가 나머지를
 * 막지 않는다.
 */
@Service
public class MyDataConsentExpiryProcessor {

  private static final Logger log = LoggerFactory.getLogger(MyDataConsentExpiryProcessor.class);

  private final MyDataTransmissionRequestRepository requestRepository;
  private final MyDataConsentEnforcementService enforcementService;
  private final MyDataConsentHistoryService consentHistoryService;
  private final FamilyAlertPort alertPort;

  public MyDataConsentExpiryProcessor(
      MyDataTransmissionRequestRepository requestRepository,
      MyDataConsentEnforcementService enforcementService,
      MyDataConsentHistoryService consentHistoryService,
      FamilyAlertPort alertPort) {
    this.requestRepository = requestRepository;
    this.enforcementService = enforcementService;
    this.consentHistoryService = consentHistoryService;
    this.alertPort = alertPort;
  }

  /** 만료 확정 — 수집 중단 + 보유 데이터 파기까지 수행한다. 이미 처리된 건은 그대로 통과한다 (멱등). */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean expire(Long requestId, LocalDateTime now) {
    MyDataTransmissionRequestEntity request = requestRepository.findById(requestId).orElse(null);
    if (request == null
        || request.getStatus() == Status.EXPIRED
        || request.getStatus() == Status.REVOKED) {
      return false;
    }
    request.setStatus(Status.EXPIRED);
    request.setRevokedAt(now);
    requestRepository.save(request);

    consentHistoryService.revokeActiveConsents(
        request.getUserId(), request.getInstitutionCode(), "동의 유효기간 만료");
    enforcementService.enforceRevocation(
        request.getUserId(), request.getInstitutionCode(), "CONSENT_EXPIRED", true);

    log.info(
        "마이데이터 동의 만료 확정 requestId={} userId={} institution={}",
        requestId,
        request.getUserId(),
        request.getInstitutionCode());
    return true;
  }

  /** 만료 예고 알림. 재동의 기회를 주기 위한 것이다. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void warnExpiringSoon(Long requestId) {
    requestRepository
        .findById(requestId)
        .ifPresent(
            request ->
                alertPort.send(
                    request.getUserId(),
                    "%s 기관의 마이데이터 연동 동의가 %s 에 만료됩니다. 계속 이용하시려면 재동의가 필요합니다."
                        .formatted(request.getInstitutionCode(), request.getConsentExpiresAt())));
  }
}
