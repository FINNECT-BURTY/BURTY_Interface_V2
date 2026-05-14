package com.berty.application.service;

import com.berty.application.port.out.AuditLogPort;
import com.berty.application.port.out.MyDataOAuthPort;
import com.berty.domain.entity.MyDataLinkStatusEntity;
import com.berty.domain.model.AuditEvent;
import com.berty.domain.repository.MyDataLinkStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class MyDataTokenRefreshBatch {
    private static final Logger log = LoggerFactory.getLogger(MyDataTokenRefreshBatch.class);
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_EXPIRED = "EXPIRED";

    private final MyDataLinkStatusRepository linkStatusRepository;
    private final MyDataOAuthPort myDataOAuthPort;
    private final AuditLogPort auditLogPort;
    private final long refreshAheadHours;

    public MyDataTokenRefreshBatch(MyDataLinkStatusRepository linkStatusRepository, MyDataOAuthPort myDataOAuthPort,
                                   AuditLogPort auditLogPort, @Value("${berty.mydata.token-refresh-ahead-hours:6}") long refreshAheadHours) {
        this.linkStatusRepository = linkStatusRepository;
        this.myDataOAuthPort = myDataOAuthPort;
        this.auditLogPort = auditLogPort;
        this.refreshAheadHours = refreshAheadHours;
    }

    @Scheduled(cron = "${berty.mydata.token-refresh-cron:0 */15 * * * *}")
    @Transactional
    public void refreshExpiringTokens() {
        LocalDateTime threshold = LocalDateTime.now().plusHours(refreshAheadHours);
        List<MyDataLinkStatusEntity> expiring = linkStatusRepository
                .findByStatusAndTokenExpiresAtBefore(STATUS_ACTIVE, threshold);
        if (expiring.isEmpty()) {
            log.debug("MyData token refresh: no tokens expiring within {}h", refreshAheadHours);
            return;
        }

        int refreshed = 0;
        int failed = 0;
        for (MyDataLinkStatusEntity entity : expiring) {
            String userId = entity.getUserId();
            try {
                String newToken = myDataOAuthPort.refreshAccessToken(userId);
                if (newToken == null || newToken.isBlank()) {
                    markExpired(entity, "EMPTY_REFRESH");
                    failed++;
                    continue;
                }
                LocalDateTime newExpiresAt = myDataOAuthPort.findTokenExpiresAt(userId);
                if (newExpiresAt != null) entity.setTokenExpiresAt(newExpiresAt);
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
        log.info("MyData token refresh batch: refreshed={} failed={} threshold={}h",
                refreshed, failed, refreshAheadHours);
        if (refreshed + failed > 0) {
            auditLogPort.save(new AuditEvent(
                    UUID.randomUUID().toString(), "system",
                    "MYDATA_TOKEN_REFRESH_BATCH", "BATCH",
                    failed == 0 ? "SUCCESS" : "PARTIAL",
                    "refreshed=" + refreshed + ",failed=" + failed,
                    LocalDateTime.now()
            ));
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
