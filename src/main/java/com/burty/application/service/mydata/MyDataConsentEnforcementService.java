package com.burty.application.service.mydata;

import com.burty.application.service.support.AuditLogger;
import com.burty.domain.asset.repository.AccountSnapshotRepository;
import com.burty.domain.finance.entity.AccountEntity;
import com.burty.domain.finance.repository.AccountRepository;
import com.burty.domain.mydata.entity.LinkedInstitutionEntity;
import com.burty.domain.mydata.repository.LinkedInstitutionRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 동의 철회·만료 시 <b>실제로 무엇을 해야 하는가</b>를 한 곳에 모은 서비스.
 *
 * <p>기존 철회 처리는 {@code tbl_mydata_transmission_request} 의 상태만 REVOKED 로 바꿨다. 즉 서류상으로만 철회되고
 *
 * <ul>
 *   <li>기관 액세스 토큰은 그대로 살아 있었고 (계속 조회 가능),
 *   <li>이미 수집한 계좌·잔액 데이터도 그대로 남아 있었다.
 * </ul>
 *
 * <p>마이데이터 사업자 등록을 목표로 한다면 이건 그냥 미구현이 아니라 규제 위반 소지다. 철회는 <b>수집 중단 + 보유 데이터 파기</b>까지가 한 단위다.
 */
@Service
public class MyDataConsentEnforcementService {

  private static final Logger log = LoggerFactory.getLogger(MyDataConsentEnforcementService.class);

  private final LinkedInstitutionPersistenceService linkedInstitutionPersistence;
  private final LinkedInstitutionRepository linkedInstitutionRepository;
  private final AccountRepository accountRepository;
  private final AccountSnapshotRepository accountSnapshotRepository;
  private final MyDataTransmissionLogService transmissionLogService;
  private final AuditLogger auditLogger;

  public MyDataConsentEnforcementService(
      LinkedInstitutionPersistenceService linkedInstitutionPersistence,
      LinkedInstitutionRepository linkedInstitutionRepository,
      AccountRepository accountRepository,
      AccountSnapshotRepository accountSnapshotRepository,
      MyDataTransmissionLogService transmissionLogService,
      AuditLogger auditLogger) {
    this.linkedInstitutionPersistence = linkedInstitutionPersistence;
    this.linkedInstitutionRepository = linkedInstitutionRepository;
    this.accountRepository = accountRepository;
    this.accountSnapshotRepository = accountSnapshotRepository;
    this.transmissionLogService = transmissionLogService;
    this.auditLogger = auditLogger;
  }

  /**
   * 동의 철회/만료를 실제로 집행한다.
   *
   * @param purgeCollectedData 수집 데이터까지 파기할지. 철회·만료는 true. 단순 재인증 대기는 false.
   * @return 파기한 계좌 수
   */
  @Transactional
  public int enforceRevocation(
      String userId, String institutionCode, String reason, boolean purgeCollectedData) {
    // 1) 토큰 무효화 — 더 이상 기관에서 데이터를 가져오지 못하게 한다.
    linkedInstitutionPersistence.markRevoked(userId, institutionCode);

    int purged = 0;
    if (purgeCollectedData) {
      purged = purgeInstitutionData(userId, institutionCode);
    }

    transmissionLogService.logOutbound(
        userId,
        institutionCode,
        "CONSENT_ENFORCED",
        "reason=%s, purgedAccounts=%d".formatted(reason, purged));
    auditLogger.logSuccess(
        userId,
        "MYDATA_CONSENT_ENFORCED",
        institutionCode,
        "reason=%s, purgedAccounts=%d".formatted(reason, purged));
    log.info(
        "마이데이터 동의 집행 완료 userId={} institution={} reason={} purgedAccounts={}",
        userId,
        institutionCode,
        reason,
        purged);
    return purged;
  }

  /** 해당 기관으로부터 수집한 계좌·스냅샷을 파기한다. */
  private int purgeInstitutionData(String userId, String institutionCode) {
    LinkedInstitutionEntity link =
        linkedInstitutionRepository
            .findByUser_UserIdAndInstitutionCode(Long.parseLong(userId), institutionCode)
            .orElse(null);
    if (link == null) {
      return 0;
    }
    List<AccountEntity> accounts =
        accountRepository.findByLinkedInstitution_LinkId(link.getLinkId());
    if (accounts.isEmpty()) {
      return 0;
    }
    // 스냅샷 → 계좌 순서로 지운다. 반대로 하면 FK 제약 위반이다.
    List<Long> accountIds = accounts.stream().map(AccountEntity::getAccountId).toList();
    long snapshots = accountSnapshotRepository.deleteByAccount_AccountIdIn(accountIds);
    accountRepository.deleteAll(accounts);
    log.info(
        "마이데이터 수집 데이터 파기 userId={} institution={} accounts={} snapshots={}",
        userId,
        institutionCode,
        accounts.size(),
        snapshots);
    return accounts.size();
  }
}
