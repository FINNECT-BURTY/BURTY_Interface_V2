package com.burty.application.service.batch;

import com.burty.application.service.support.AuditLogger;
import com.burty.domain.user.entity.DataErasureRequestEntity;
import com.burty.domain.user.entity.DataErasureRequestEntity.Status;
import com.burty.domain.user.repository.DataErasureRequestRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 법정 보존기간이 끝난 개인정보 파기.
 *
 * <p>탈퇴 시점에 모든 데이터를 지우면 전자금융거래법상 보존의무를 위반한다. 반대로 영원히 들고 있으면 개인정보보호법 위반이다. 그래서 탈퇴 시 {@code
 * tbl_data_erasure_request} 에 보존 만료 시각을 기록해 두고, 이 배치가 그 시점에 잔여 데이터를 파기한다.
 *
 * <p>여기서 지우는 것은 <b>이미 익명화된 사용자에 매달린 잔여 기록</b>이다. 탈퇴 시점에 직접 식별정보는 이미 제거됐으므로, 이 단계는 "익명화된 껍데기까지 정리"
 * 하는 마무리다.
 */
@Service
public class PersonalDataRetentionBatch {

  private static final Logger log = LoggerFactory.getLogger(PersonalDataRetentionBatch.class);

  private final DataErasureRequestRepository erasureRepository;
  private final PersonalDataPurger purger;
  private final AuditLogger auditLogger;
  private final Clock clock;

  public PersonalDataRetentionBatch(
      DataErasureRequestRepository erasureRepository,
      PersonalDataPurger purger,
      AuditLogger auditLogger,
      Clock clock) {
    this.erasureRepository = erasureRepository;
    this.purger = purger;
    this.auditLogger = auditLogger;
    this.clock = clock;
  }

  @Scheduled(cron = "${burty.privacy.retention-purge-cron:0 0 3 * * *}")
  @SchedulerLock(name = "personalDataRetention", lockAtMostFor = "PT2H", lockAtLeastFor = "PT1M")
  @Transactional(readOnly = true)
  public void purgeExpiredRetention() {
    LocalDateTime now = LocalDateTime.now(clock);
    List<DataErasureRequestEntity> due =
        erasureRepository.findByStatusAndRetentionUntilLessThanEqualOrderByErasureIdAsc(
            Status.IMMEDIATE_DONE, now);
    if (due.isEmpty()) {
      return;
    }
    log.info("보존기간 만료 개인정보 파기 시작 — 대상 {}건", due.size());

    for (DataErasureRequestEntity request : due) {
      try {
        String summary = purger.purge(request.getErasureId(), now);
        auditLogger.logSuccess(
            String.valueOf(request.getUserId()),
            "PERSONAL_DATA_PURGED",
            String.valueOf(request.getUserId()),
            "보존기간 만료 파기: " + summary);
      } catch (RuntimeException e) {
        log.error(
            "보존기간 만료 파기 실패 erasureId={} userId={} reason={}",
            request.getErasureId(),
            request.getUserId(),
            e.getMessage(),
            e);
      }
    }
  }
}
