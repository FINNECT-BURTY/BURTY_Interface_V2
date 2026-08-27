package com.burty.application.service.admin;

import com.burty.application.service.support.AuditChainHasher;
import com.burty.domain.admin.entity.AuditLogEntity;
import com.burty.domain.admin.repository.AuditLogRepository;
import java.util.ArrayList;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 감사 로그 해시 체인 검증.
 *
 * <p>기록을 남기는 것만으로는 부족하다. 아무도 검증하지 않는 체인은 없는 것과 같다. 정기적으로 전체를 훑어 다음을 확인한다.
 *
 * <ul>
 *   <li>각 행의 entryHash 가 내용과 일치하는가 (내용 변조 탐지)
 *   <li>각 행의 prevHash 가 직전 행의 entryHash 와 이어지는가 (연결 변조 탐지)
 *   <li>chainSeq 가 빠짐없이 이어지는가 (행 삭제 탐지)
 * </ul>
 */
@Service
public class AuditChainVerifier {

  private static final Logger log = LoggerFactory.getLogger(AuditChainVerifier.class);
  private static final int CHUNK = 500;

  private final AuditLogRepository auditLogRepository;

  public AuditChainVerifier(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  /** 검증 결과. */
  public record VerificationResult(long verified, List<String> violations) {
    public boolean intact() {
      return violations.isEmpty();
    }
  }

  @Scheduled(cron = "${burty.audit.verify-cron:0 30 4 * * *}")
  @SchedulerLock(name = "auditChainVerify", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
  public void scheduledVerify() {
    VerificationResult result = verify();
    if (result.intact()) {
      log.info("감사 로그 체인 검증 통과 — {}건", result.verified());
      return;
    }
    // 이건 조용히 넘어갈 사안이 아니다. 감사 기록이 변조됐다는 뜻이다.
    log.error(
        "감사 로그 체인 무결성 위반 감지 — {}건 검증 중 {}건 이상 (상세: {})",
        result.verified(),
        result.violations().size(),
        result.violations().subList(0, Math.min(10, result.violations().size())));
  }

  @Transactional(readOnly = true)
  public VerificationResult verify() {
    List<String> violations = new ArrayList<>();
    long verified = 0;
    String expectedPrevHash = AuditChainHasher.GENESIS;
    long expectedSeq = 1;

    long maxSeq =
        auditLogRepository
            .findTopByOrderByChainSeqDesc()
            .map(AuditLogEntity::getChainSeq)
            .orElse(0L);

    for (long from = 1; from <= maxSeq; from += CHUNK) {
      long to = Math.min(from + CHUNK - 1, maxSeq);
      List<AuditLogEntity> chunk =
          auditLogRepository.findByChainSeqBetweenOrderByChainSeqAsc(from, to);

      for (AuditLogEntity entry : chunk) {
        if (entry.getChainSeq() == null) {
          continue; // 체인 도입 이전 데이터
        }
        if (entry.getChainSeq() != expectedSeq) {
          violations.add(
              "순번 불연속: expected=%d actual=%d (auditId=%d)"
                  .formatted(expectedSeq, entry.getChainSeq(), entry.getAuditId()));
          expectedSeq = entry.getChainSeq();
        }
        if (!expectedPrevHash.equals(entry.getPrevHash())) {
          violations.add(
              "체인 연결 불일치: auditId=%d prevHash=%s expected=%s"
                  .formatted(entry.getAuditId(), entry.getPrevHash(), expectedPrevHash));
        }
        String recomputed = AuditChainHasher.hash(entry, entry.getPrevHash());
        if (!recomputed.equals(entry.getEntryHash())) {
          violations.add(
              "내용 변조 의심: auditId=%d storedHash=%s recomputed=%s"
                  .formatted(entry.getAuditId(), entry.getEntryHash(), recomputed));
        }

        expectedPrevHash =
            entry.getEntryHash() == null ? AuditChainHasher.GENESIS : entry.getEntryHash();
        expectedSeq = entry.getChainSeq() + 1;
        verified++;
      }
    }
    return new VerificationResult(verified, violations);
  }
}
