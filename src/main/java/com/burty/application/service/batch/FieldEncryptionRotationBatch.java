package com.burty.application.service.batch;

import com.burty.application.service.support.AuditLogger;
import com.burty.config.FieldEncryptionProperties;
import com.burty.domain.mydata.entity.LinkedInstitutionEntity;
import com.burty.domain.mydata.repository.LinkedInstitutionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 필드 암호화 키 로테이션.
 *
 * <p>{@code FieldEncryptor} 는 버전 바이트로 어느 키로 암호화됐는지 구분하므로, 구키로 쓴 값도 계속 읽을 수 있다. 하지만 읽기만 되고 다시 쓰지 않으면
 * 구키를 영원히 들고 있어야 한다. 그러면 로테이션의 의미가 없다.
 *
 * <p>이 배치가 구키로 암호화된 행을 찾아 현재 키로 다시 쓴다. 전부 옮기고 나면 구키 설정을 제거할 수 있다.
 *
 * <p>대상은 {@code tbl_linked_institution} 의 마이데이터 토큰이다. Redis·인메모리 토큰 스토어는 TTL 로 자연히 교체되므로 따로 다루지
 * 않는다.
 *
 * <p><b>진행 방식</b> — PK 순서로 한 페이지씩 훑고, 한 주기에 처리할 상한을 둔다. 운영 중에 돌아도 부하가 튀지 않게 하기 위함이고, 중간에 멈춰도 다음 주기에
 * 이어서 하면 된다(재암호화는 멱등하다).
 */
@Service
public class FieldEncryptionRotationBatch {

  private static final Logger log = LoggerFactory.getLogger(FieldEncryptionRotationBatch.class);

  private final LinkedInstitutionRepository linkedInstitutionRepository;
  private final FieldEncryptionRotationWriter writer;
  private final FieldEncryptionProperties properties;
  private final AuditLogger auditLogger;
  private final ObjectProvider<MeterRegistry> meterRegistry;

  public FieldEncryptionRotationBatch(
      LinkedInstitutionRepository linkedInstitutionRepository,
      FieldEncryptionRotationWriter writer,
      FieldEncryptionProperties properties,
      AuditLogger auditLogger,
      ObjectProvider<MeterRegistry> meterRegistry) {
    this.linkedInstitutionRepository = linkedInstitutionRepository;
    this.writer = writer;
    this.properties = properties;
    this.auditLogger = auditLogger;
    this.meterRegistry = meterRegistry;
  }

  /** 로테이션 한 주기의 결과. */
  public record RotationResult(int scanned, int rotated, int failed) {
    public boolean hasWork() {
      return scanned > 0;
    }
  }

  @Scheduled(cron = "${burty.security.field-encryption-rotation.cron:0 15 3 * * *}")
  @SchedulerLock(name = "fieldEncryptionRotation", lockAtMostFor = "PT2H", lockAtLeastFor = "PT1M")
  public void run() {
    if (!properties.isEnabled()) {
      return;
    }
    RotationResult result = rotateOnce();
    if (result.hasWork()) {
      log.info(
          "필드 암호화 로테이션 — 검사 {} / 재암호화 {} / 실패 {}",
          result.scanned(),
          result.rotated(),
          result.failed());
      auditLogger.logSuccess(
          "SYSTEM",
          "FIELD_ENCRYPTION_ROTATION",
          "tbl_linked_institution",
          "scanned=%d, rotated=%d, failed=%d"
              .formatted(result.scanned(), result.rotated(), result.failed()));
    }
    count(result);
  }

  /**
   * 한 주기 실행. 스케줄·락과 분리해 테스트에서 직접 호출할 수 있게 한다.
   *
   * @return 처리 결과
   */
  public RotationResult rotateOnce() {
    long cursor = 0;
    int scanned = 0;
    int rotated = 0;
    int failed = 0;

    while (scanned < properties.getMaxRowsPerRun()) {
      List<LinkedInstitutionEntity> page =
          linkedInstitutionRepository.findByLinkIdGreaterThanOrderByLinkIdAsc(
              cursor, Limit.of(properties.getBatchSize()));
      if (page.isEmpty()) {
        break;
      }
      for (LinkedInstitutionEntity link : page) {
        cursor = link.getLinkId();
        scanned++;
        try {
          if (writer.rotate(link.getLinkId())) {
            rotated++;
          }
        } catch (RuntimeException e) {
          // 한 행이 실패해도 나머지는 계속한다. 복호화 불가는 키 설정 문제일 수 있으므로
          // 조용히 넘기지 않고 남긴다.
          failed++;
          log.error(
              "필드 암호화 재암호화 실패 — 수동 확인 필요 linkId={} reason={}", link.getLinkId(), e.getMessage(), e);
        }
      }
    }
    return new RotationResult(scanned, rotated, failed);
  }

  private void count(RotationResult result) {
    MeterRegistry registry = meterRegistry.getIfAvailable();
    if (registry == null) {
      return;
    }
    registry.counter("burty.encryption.rotation", "outcome", "rotated").increment(result.rotated());
    registry.counter("burty.encryption.rotation", "outcome", "failed").increment(result.failed());
  }
}
