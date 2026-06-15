/**
 *
 *
 * <pre>
 * <b>Description  : 배치 배치 작업 (TransactionSyncBatch)</b>
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

import com.burty.application.port.in.transaction.TransactionSyncUseCase;
import com.burty.application.service.support.AuditLogger;
import com.burty.core.constant.LogMessages;
import com.burty.domain.mydata.entity.MyDataLinkStatusEntity;
import com.burty.domain.mydata.repository.MyDataLinkStatusRepository;
import com.burty.domain.user.entity.UserSettingEntity;
import com.burty.domain.user.repository.UserSettingRepository;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TransactionSyncBatch {
  private static final Logger log = LoggerFactory.getLogger(TransactionSyncBatch.class);
  private static final String FINTECH_KEY = "FINTECH_USE_NUM";

  private final MyDataLinkStatusRepository linkStatusRepository;
  private final TransactionSyncUseCase transactionSyncUseCase;
  private final AuditLogger auditLogger;
  private final UserSettingRepository userSettingRepository;
  private final String defaultFintechUseNum;

  public TransactionSyncBatch(
      MyDataLinkStatusRepository linkStatusRepository,
      TransactionSyncUseCase transactionSyncUseCase,
      AuditLogger auditLogger,
      UserSettingRepository userSettingRepository,
      @Value("${burty.transaction.sync-default-fintech-use-num:DEFAULT}")
          String defaultFintechUseNum) {
    this.linkStatusRepository = linkStatusRepository;
    this.transactionSyncUseCase = transactionSyncUseCase;
    this.auditLogger = auditLogger;
    this.userSettingRepository = userSettingRepository;
    this.defaultFintechUseNum = defaultFintechUseNum;
  }

  @Scheduled(cron = "${burty.transaction.sync-cron:0 0 4 * * *}")
  @SchedulerLock(name = "TransactionSyncBatch", lockAtLeastFor = "PT5M", lockAtMostFor = "PT55M")
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
        String fintechUseNum =
            userSettingRepository
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
    log.info(LogMessages.Batch.TRANSACTION_SYNC, active.size(), success, failed, totalSaved);
    auditLogger.log(
        "system",
        "TRANSACTION_SYNC_BATCH",
        "BATCH",
        failed == 0 ? "SUCCESS" : "PARTIAL",
        "users=" + active.size() + ",savedTotal=" + totalSaved + ",failed=" + failed);
  }
}
