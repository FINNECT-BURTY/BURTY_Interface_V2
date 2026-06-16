package com.burty.application.service.mydata;

import com.burty.application.port.in.mydata.MyDataAuthUseCase;
import com.burty.application.port.out.mydata.MyDataOAuthPort;
import com.burty.application.service.support.AuditLogger;
import com.burty.config.MyDataProperties;
import com.burty.core.constant.AppMessages;
import com.burty.domain.mydata.entity.MyDataLinkStatusEntity;
import com.burty.domain.mydata.model.MyDataTokenBundle;
import com.burty.domain.mydata.repository.MyDataLinkStatusRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MyDataAuthService implements MyDataAuthUseCase {
  private static final Logger log = LoggerFactory.getLogger(MyDataAuthService.class);
  private static final String DEFAULT_INSTITUTION = "MYDATA";

  private final MyDataOAuthPort myDataOAuthPort;
  private final MyDataLinkStatusRepository linkStatusRepository;
  private final LinkedInstitutionPersistenceService linkedInstitutionPersistence;
  private final MyDataTransmissionService transmissionService;
  private final MyDataTransmissionLogService transmissionLogService;
  private final MyDataConsentHistoryService consentHistoryService;
  private final FinanceOAuthStateService financeOAuthStateService;
  private final MyDataTokenHydrationService tokenHydrationService;
  private final MyDataProperties myDataProperties;
  private final AuditLogger auditLogger;

  public MyDataAuthService(
      MyDataOAuthPort myDataOAuthPort,
      MyDataLinkStatusRepository linkStatusRepository,
      LinkedInstitutionPersistenceService linkedInstitutionPersistence,
      MyDataTransmissionService transmissionService,
      MyDataTransmissionLogService transmissionLogService,
      MyDataConsentHistoryService consentHistoryService,
      FinanceOAuthStateService financeOAuthStateService,
      MyDataTokenHydrationService tokenHydrationService,
      MyDataProperties myDataProperties,
      AuditLogger auditLogger) {
    this.myDataOAuthPort = myDataOAuthPort;
    this.linkStatusRepository = linkStatusRepository;
    this.linkedInstitutionPersistence = linkedInstitutionPersistence;
    this.transmissionService = transmissionService;
    this.transmissionLogService = transmissionLogService;
    this.consentHistoryService = consentHistoryService;
    this.financeOAuthStateService = financeOAuthStateService;
    this.tokenHydrationService = tokenHydrationService;
    this.myDataProperties = myDataProperties;
    this.auditLogger = auditLogger;
  }

  @Override
  public String createAuthorizeUrl(String userId) {
    return createAuthorizeUrl(userId, DEFAULT_INSTITUTION);
  }

  @Override
  public String createAuthorizeUrl(String userId, String institutionCode) {
    String inst = nullOrBlank(institutionCode) ? DEFAULT_INSTITUTION : institutionCode;
    String oauthState =
        financeOAuthStateService.issue(FinanceOAuthStateService.PROVIDER_MYDATA, userId, inst);
    return myDataOAuthPort.buildAuthorizeUrl(oauthState);
  }

  @Override
  @Transactional
  public boolean exchangeAuthorizationCode(String userId, String code) {
    return exchangeAuthorizationCode(userId, DEFAULT_INSTITUTION, code);
  }

  @Override
  @Transactional
  public boolean exchangeAuthorizationCode(String userId, String institutionCode, String code) {
    String inst = nullOrBlank(institutionCode) ? DEFAULT_INSTITUTION : institutionCode;
    return doExchange(userId, inst, code);
  }

  @Override
  @Transactional
  public boolean exchangeAuthorizationCodeByState(String state, String code) {
    FinanceOAuthStateService.FinanceOAuthContext ctx =
        financeOAuthStateService.consume(FinanceOAuthStateService.PROVIDER_MYDATA, state);
    return doExchange(ctx.userId(), ctx.institutionCode(), code);
  }

  private boolean doExchange(String userId, String institutionCode, String code) {
    String scopeKey = MyDataOAuthPort.scopeKey(userId, institutionCode);
    try {
      MyDataTokenBundle bundle = myDataOAuthPort.exchangeTokens(scopeKey, code);
      boolean success =
          bundle != null && bundle.accessToken() != null && !bundle.accessToken().isBlank();
      if (success) {
        linkedInstitutionPersistence.saveTokens(userId, institutionCode, bundle);
        recordStatus(userId, institutionCode, "ACTIVE", null, bundle.tokenExpiresAt());
        transmissionService.activateAfterOAuth(
            userId, institutionCode, myDataProperties.getScope());
        transmissionLogService.logInbound(userId, institutionCode, "TOKEN_EXCHANGE", "success");
      } else {
        recordStatus(userId, institutionCode, "FAILED", "EMPTY_TOKEN", null);
        transmissionLogService.logInbound(userId, institutionCode, "TOKEN_EXCHANGE", "empty token");
      }
      auditLogger.log(
          userId,
          "MYDATA_TOKEN_EXCHANGE",
          institutionCode,
          success ? "SUCCESS" : "FAILURE",
          success ? "linked" : "no token returned");
      return success;
    } catch (Exception e) {
      log.warn(AppMessages.MyData.TOKEN_EXCHANGE_WARN, userId, institutionCode, e.getMessage());
      recordStatus(userId, institutionCode, "FAILED", classify(e), null);
      transmissionLogService.logInbound(userId, institutionCode, "TOKEN_EXCHANGE", e.getMessage());
      auditLogger.log(
          userId,
          "MYDATA_TOKEN_EXCHANGE",
          institutionCode,
          "FAILURE",
          "exception=" + e.getClass().getSimpleName());
      return false;
    }
  }

  @Override
  public List<MyDataLinkStatusEntity> listInstitutions(String userId) {
    return linkStatusRepository.findByUserId(userId);
  }

  @Override
  @Transactional
  public boolean unlinkInstitution(String userId, String institutionCode) {
    String inst = nullOrBlank(institutionCode) ? DEFAULT_INSTITUTION : institutionCode;
    return linkStatusRepository
        .findByUserIdAndInstitutionCode(userId, inst)
        .map(
            entity -> {
              entity.setStatus("UNLINKED");
              entity.setUnlinkedAt(LocalDateTime.now());
              linkStatusRepository.save(entity);
              linkedInstitutionPersistence.markRevoked(userId, inst);
              tokenHydrationService.clearRuntimeTokens(userId, inst);
              consentHistoryService.revokeActiveConsents(userId, inst, "사용자 연동 해제");
              transmissionLogService.logOutbound(userId, inst, "UNLINK", "user requested");
              auditLogger.logSuccess(userId, "MYDATA_UNLINK", inst, "institutionCode=" + inst);
              return true;
            })
        .orElse(false);
  }

  private void recordStatus(
      String userId,
      String institutionCode,
      String status,
      String errorCode,
      LocalDateTime tokenExpiresAt) {
    MyDataLinkStatusEntity entity =
        linkStatusRepository
            .findByUserIdAndInstitutionCode(userId, institutionCode)
            .orElseGet(MyDataLinkStatusEntity::new);
    entity.setUserId(userId);
    entity.setInstitutionCode(institutionCode);
    entity.setStatus(status);
    entity.setLastErrorCode(errorCode);
    if ("ACTIVE".equals(status)) {
      entity.setLinkedAt(LocalDateTime.now());
      entity.setTokenExpiresAt(tokenExpiresAt);
    }
    linkStatusRepository.save(entity);
  }

  private static boolean nullOrBlank(String value) {
    return value == null || value.isBlank();
  }

  private static String classify(Exception e) {
    return e.getClass().getSimpleName();
  }
}
