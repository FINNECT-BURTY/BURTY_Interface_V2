/**
 *
 *
 * <pre>
 * <b>Description  : 배치 배치 작업 (MyDataTokenRefreshBatch)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.batch
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
package com.burty.application.service.batch;

import com.burty.application.port.out.mydata.MyDataOAuthPort;
import com.burty.application.service.mydata.LinkedInstitutionPersistenceService;
import com.burty.application.service.mydata.MyDataTokenHydrationService;
import com.burty.application.service.support.AuditLogger;
import com.burty.core.constant.LogMessages;
import com.burty.domain.mydata.entity.MyDataLinkStatusEntity;
import com.burty.domain.mydata.model.MyDataTokenBundle;
import com.burty.domain.mydata.repository.MyDataLinkStatusRepository;
import java.time.LocalDateTime;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MyDataTokenRefreshBatch {
  private static final Logger log = LoggerFactory.getLogger(MyDataTokenRefreshBatch.class);
  private static final String STATUS_ACTIVE = "ACTIVE";
  private static final String STATUS_EXPIRED = "EXPIRED";

  private final MyDataLinkStatusRepository linkStatusRepository;
  private final MyDataOAuthPort myDataOAuthPort;
  private final LinkedInstitutionPersistenceService linkedInstitutionPersistence;
  private final MyDataTokenHydrationService tokenHydrationService;
  private final AuditLogger auditLogger;
  private final long refreshAheadHours;

  public MyDataTokenRefreshBatch(
      MyDataLinkStatusRepository linkStatusRepository,
      MyDataOAuthPort myDataOAuthPort,
      LinkedInstitutionPersistenceService linkedInstitutionPersistence,
      MyDataTokenHydrationService tokenHydrationService,
      AuditLogger auditLogger,
      @Value("${burty.mydata.token-refresh-ahead-hours:6}") long refreshAheadHours) {
    this.linkStatusRepository = linkStatusRepository;
    this.myDataOAuthPort = myDataOAuthPort;
    this.linkedInstitutionPersistence = linkedInstitutionPersistence;
    this.tokenHydrationService = tokenHydrationService;
    this.auditLogger = auditLogger;
    this.refreshAheadHours = refreshAheadHours;
  }

  @Scheduled(cron = "${burty.mydata.token-refresh-cron:0 */15 * * * *}")
  @SchedulerLock(name = "MyDataTokenRefreshBatch", lockAtLeastFor = "PT1M", lockAtMostFor = "PT14M")
  @Transactional
  public void refreshExpiringTokens() {
    LocalDateTime threshold = LocalDateTime.now().plusHours(refreshAheadHours);
    List<MyDataLinkStatusEntity> expiring =
        linkStatusRepository.findByStatusAndTokenExpiresAtBefore(STATUS_ACTIVE, threshold);
    if (expiring.isEmpty()) {
      log.debug("MyData token refresh: no tokens expiring within {}h", refreshAheadHours);
      return;
    }

    int refreshed = 0;
    int failed = 0;
    for (MyDataLinkStatusEntity entity : expiring) {
      String userId = entity.getUserId();
      String institutionCode = entity.getInstitutionCode();
      try {
        String scopeKey = MyDataOAuthPort.scopeKey(userId, institutionCode);
        tokenHydrationService.hydrate(userId, institutionCode);
        String newToken = myDataOAuthPort.refreshAccessToken(scopeKey);
        if (newToken == null || newToken.isBlank()) {
          markExpired(entity, "EMPTY_REFRESH");
          failed++;
          continue;
        }
        LocalDateTime newExpiresAt = myDataOAuthPort.findTokenExpiresAt(scopeKey);
        if (newExpiresAt != null) {
          entity.setTokenExpiresAt(newExpiresAt);
        }
        linkedInstitutionPersistence.saveTokens(
            userId,
            institutionCode,
            new MyDataTokenBundle(
                newToken, myDataOAuthPort.findRefreshToken(scopeKey), newExpiresAt));
        entity.setLastErrorCode(null);
        entity.setLastErrorAt(null);
        linkStatusRepository.save(entity);
        refreshed++;
      } catch (Exception e) {
        log.warn("Token refresh failed userId={} err={}", userId, e.getMessage());
        markExpired(entity, classify(e));
        failed++;
      }
    }
    log.info(LogMessages.Batch.MYDATA_TOKEN_REFRESH, refreshed, failed, refreshAheadHours);
    if (refreshed + failed > 0) {
      auditLogger.log(
          "system",
          "MYDATA_TOKEN_REFRESH_BATCH",
          "BATCH",
          failed == 0 ? "SUCCESS" : "PARTIAL",
          "refreshed=" + refreshed + ",failed=" + failed);
    }
  }

  private void markExpired(MyDataLinkStatusEntity entity, String errorCode) {
    entity.setStatus(STATUS_EXPIRED);
    entity.setLastErrorCode(errorCode);
    entity.setLastErrorAt(LocalDateTime.now());
    linkStatusRepository.save(entity);
  }

  private String classify(Exception e) {
    String simple = e.getClass().getSimpleName();
    if (simple.contains("Timeout")) return "TIMEOUT";
    if (simple.contains("Unauthorized") || simple.contains("Forbidden")) return "AUTH";
    return "UNKNOWN";
  }
}
