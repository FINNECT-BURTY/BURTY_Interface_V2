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
  private static final String STATUS_REVOKED = "REVOKED";

  private final MyDataLinkStatusRepository linkStatusRepository;
  private final MyDataOAuthPort myDataOAuthPort;
  private final LinkedInstitutionPersistenceService linkedInstitutionPersistence;
  private final MyDataTokenHydrationService tokenHydrationService;
  private final AuditLogger auditLogger;
  private final long refreshAheadHours;

  /**
   * 마지막 성공 시각 (epoch seconds).
   *
   * <p>이 배치가 조용히 멈추면 마이데이터 연동이 며칠 뒤에야 끊긴 것을 알게 된다. "실패 카운터" 만으로는 <b>아예 돌지 않는 상태</b>를 탐지할 수 없어서, 마지막
   * 성공 시각을 게이지로 노출하고 알람에서 경과 시간을 본다.
   */
  private final java.util.concurrent.atomic.AtomicLong lastSuccessEpochSeconds =
      new java.util.concurrent.atomic.AtomicLong(0);

  public MyDataTokenRefreshBatch(
      MyDataLinkStatusRepository linkStatusRepository,
      MyDataOAuthPort myDataOAuthPort,
      LinkedInstitutionPersistenceService linkedInstitutionPersistence,
      MyDataTokenHydrationService tokenHydrationService,
      AuditLogger auditLogger,
      org.springframework.beans.factory.ObjectProvider<io.micrometer.core.instrument.MeterRegistry>
          meterRegistry,
      @Value("${burty.mydata.token-refresh-ahead-hours:6}") long refreshAheadHours) {
    this.linkStatusRepository = linkStatusRepository;
    this.myDataOAuthPort = myDataOAuthPort;
    this.linkedInstitutionPersistence = linkedInstitutionPersistence;
    this.tokenHydrationService = tokenHydrationService;
    this.auditLogger = auditLogger;
    this.refreshAheadHours = refreshAheadHours;

    io.micrometer.core.instrument.MeterRegistry registry = meterRegistry.getIfAvailable();
    if (registry != null) {
      io.micrometer.core.instrument.Gauge.builder(
              "burty.mydata.token.refresh.last.success.timestamp",
              lastSuccessEpochSeconds,
              java.util.concurrent.atomic.AtomicLong::get)
          .description("마이데이터 토큰 갱신 배치가 마지막으로 성공한 시각 (epoch seconds)")
          .baseUnit("seconds")
          .register(registry);
    }
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
    int skipped = 0;
    for (MyDataLinkStatusEntity entity : expiring) {
      String userId = entity.getUserId();
      String institutionCode = entity.getInstitutionCode();
      try {
        // 링크 상태가 ACTIVE 여도 원본(DB) 연동이 ACTIVE 가 아니면 갱신하지 않는다. 두 테이블이
        // 어긋나면 철회한 연동에 새 토큰을 받게 된다(#142). 어긋난 상태는 여기서 바로잡는다.
        if (linkedInstitutionPersistence.loadTokenBundle(userId, institutionCode).isEmpty()) {
          markNotActive(entity);
          tokenHydrationService.clearRuntimeTokens(userId, institutionCode);
          skipped++;
          continue;
        }
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
        // saveTokens 를 쓰면 안 된다. 연결 시점용이라 상태를 ACTIVE 로 되돌리고 동의 만료일을
        // 다시 쓴다 — 철회가 되돌려지고 동의 기간이 갱신 때마다 밀렸다.
        boolean applied =
            linkedInstitutionPersistence.updateRefreshedTokens(
                userId,
                institutionCode,
                new MyDataTokenBundle(
                    newToken, myDataOAuthPort.findRefreshToken(scopeKey), newExpiresAt));
        if (!applied) {
          // 갱신하는 사이에 철회됐다. 받아 온 토큰은 쓰지 않는다.
          markNotActive(entity);
          tokenHydrationService.clearRuntimeTokens(userId, institutionCode);
          skipped++;
          continue;
        }
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
    lastSuccessEpochSeconds.set(java.time.Instant.now().getEpochSecond());
    log.info(LogMessages.Batch.MYDATA_TOKEN_REFRESH, refreshed, failed, refreshAheadHours);
    if (skipped > 0) {
      log.info("MyData token refresh skipped {} link(s) whose source link is not active", skipped);
    }
    if (refreshed + failed + skipped > 0) {
      auditLogger.log(
          "system",
          "MYDATA_TOKEN_REFRESH_BATCH",
          "BATCH",
          failed == 0 ? "SUCCESS" : "PARTIAL",
          "refreshed=" + refreshed + ",failed=" + failed + ",skipped=" + skipped);
    }
  }

  private void markExpired(MyDataLinkStatusEntity entity, String errorCode) {
    entity.setStatus(STATUS_EXPIRED);
    entity.setLastErrorCode(errorCode);
    entity.setLastErrorAt(LocalDateTime.now());
    linkStatusRepository.save(entity);
  }

  private void markNotActive(MyDataLinkStatusEntity entity) {
    entity.setStatus(STATUS_REVOKED);
    entity.setLastErrorCode("LINK_NOT_ACTIVE");
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
