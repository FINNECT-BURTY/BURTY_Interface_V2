package com.burty.application.service;

import com.burty.application.port.in.TransactionSyncUseCase;
import com.burty.application.port.out.AuditLogPort;
import com.burty.domain.entity.MyDataLinkStatusEntity;
import com.burty.domain.entity.UserSettingEntity;
import com.burty.domain.model.AuditEvent;
import com.burty.domain.repository.MyDataLinkStatusRepository;
import com.burty.domain.repository.UserSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class TransactionSyncBatch {
    private static final Logger log = LoggerFactory.getLogger(TransactionSyncBatch.class);
    private static final String FINTECH_KEY = "FINTECH_USE_NUM";

    private final MyDataLinkStatusRepository linkStatusRepository;
    private final TransactionSyncUseCase transactionSyncUseCase;
    private final AuditLogPort auditLogPort;
    private final UserSettingRepository userSettingRepository;
    private final String defaultFintechUseNum;

    public TransactionSyncBatch(MyDataLinkStatusRepository linkStatusRepository,
                                TransactionSyncUseCase transactionSyncUseCase,
                                AuditLogPort auditLogPort,
                                UserSettingRepository userSettingRepository,
                                @Value("${burty.transaction.sync-default-fintech-use-num:DEFAULT}") String defaultFintechUseNum) {
        this.linkStatusRepository = linkStatusRepository;
        this.transactionSyncUseCase = transactionSyncUseCase;
        this.auditLogPort = auditLogPort;
        this.userSettingRepository = userSettingRepository;
        this.defaultFintechUseNum = defaultFintechUseNum;
    }

    @Scheduled(cron = "${burty.transaction.sync-cron:0 0 4 * * *}")
    public void syncDaily() {
        List<MyDataLinkStatusEntity> active = linkStatusRepository.findByStatus("ACTIVE");
        if (active.isEmpty()) {
            log.debug("Transaction sync batch: no active links");
            return;
        }
        int totalSaved = 0;
        int success = 0;
        int failed = 0;
        for (MyDataLinkStatusEntity entity : active) {
            try {
                String fintechUseNum = userSettingRepository
                        .findByUserIdAndSettingKey(entity.getUserId(), FINTECH_KEY)
                        .map(UserSettingEntity::getSettingValueStr)
                        .filter(v -> v != null && !v.isBlank())
                        .orElse(defaultFintechUseNum);
                int saved = transactionSyncUseCase.syncFromOpenBanking(entity.getUserId(), fintechUseNum);
                totalSaved += saved;
                success++;
            } catch (Exception e) {
                log.warn("Transaction sync failed userId={} err={}", entity.getUserId(), e.getMessage());
                failed++;
            }
        }
        log.info("Transaction sync batch: users={} success={} failed={} savedTotal={}",
                active.size(), success, failed, totalSaved);
        auditLogPort.save(new AuditEvent(
                UUID.randomUUID().toString(), "system",
                "TRANSACTION_SYNC_BATCH", "BATCH",
                failed == 0 ? "SUCCESS" : "PARTIAL",
                "users=" + active.size() + ",savedTotal=" + totalSaved + ",failed=" + failed,
                LocalDateTime.now()
        ));
    }
}
