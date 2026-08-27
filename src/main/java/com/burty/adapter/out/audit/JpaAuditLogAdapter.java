package com.burty.adapter.out.audit;

import com.burty.application.port.out.audit.AuditLogPort;
import com.burty.application.service.support.AuditChainHasher;
import com.burty.domain.admin.entity.AuditLogEntity;
import com.burty.domain.admin.model.AuditEvent;
import com.burty.domain.admin.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 감사 로그 저장 (append-only 해시 체인).
 *
 * <p>각 행은 직전 행의 해시를 품는다. 중간 행을 고치거나 지우면 그 이후 체인이 전부 어긋나므로, DB 를 직접 만질 수 있는 사람이라도 흔적 없이 감사 기록을 바꿀 수
 * 없다. 검증은 {@code AuditChainVerifier} 가 수행한다.
 *
 * <p>{@code REQUIRES_NEW} 인 이유: 체인은 순번이 이어져야 한다. 호출한 비즈니스 트랜잭션이 롤백돼도 "그 시도가 있었다" 는 사실은 남아야 하고, 반대로
 * 감사 저장 실패가 비즈니스 트랜잭션을 깨서도 안 된다.
 */
@Primary
@Component
public class JpaAuditLogAdapter implements AuditLogPort {

  private final AuditLogRepository auditLogRepository;
  private final ObjectMapper objectMapper;

  public JpaAuditLogAdapter(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
    this.auditLogRepository = auditLogRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void save(AuditEvent event) {
    AuditLogEntity entity = new AuditLogEntity();
    entity.setOccurredAt(event.createdAt());
    entity.setActorType(AuditLogEntity.ActorType.USER);
    entity.setActorId(parseLongOrNull(event.actorId()));
    entity.setTargetType(event.target() == null ? "BURTY" : event.target());
    entity.setAction(event.action());
    entity.setResult(toResult(event.result()));
    entity.setRequestId(event.traceId());
    entity.setMetadata(toJson(event.detail()));

    AuditLogEntity previous = auditLogRepository.findTopByOrderByChainSeqDesc().orElse(null);
    long nextSeq =
        previous == null || previous.getChainSeq() == null ? 1L : previous.getChainSeq() + 1;
    String prevHash =
        previous == null || previous.getEntryHash() == null
            ? AuditChainHasher.GENESIS
            : previous.getEntryHash();

    entity.setChainSeq(nextSeq);
    entity.setPrevHash(prevHash);
    entity.setEntryHash(AuditChainHasher.hash(entity, prevHash));

    auditLogRepository.save(entity);
  }

  /**
   * detail 을 JSON 으로 만든다.
   *
   * <p>예전에는 문자열 연결로 JSON 을 조립하면서 따옴표만 작은따옴표로 바꿨다. 개행이나 백슬래시가 들어오면 깨진 JSON 이 저장됐고, 그게 그대로 해시 대상이 되면
   * 검증도 불안정해진다.
   */
  private String toJson(String detail) {
    try {
      return objectMapper.writeValueAsString(Map.of("detail", detail == null ? "" : detail));
    } catch (JsonProcessingException e) {
      return "{\"detail\":\"\"}";
    }
  }

  private AuditLogEntity.Result toResult(String result) {
    if ("FAILED".equalsIgnoreCase(result)) return AuditLogEntity.Result.FAILED;
    if ("DENIED".equalsIgnoreCase(result)) return AuditLogEntity.Result.DENIED;
    return AuditLogEntity.Result.SUCCESS;
  }

  private Long parseLongOrNull(String value) {
    try {
      return value == null ? null : Long.parseLong(value);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }
}
