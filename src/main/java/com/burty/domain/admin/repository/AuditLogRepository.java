package com.burty.domain.admin.repository;

import com.burty.domain.admin.entity.AuditLogEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

  /** 체인의 마지막 행. 새 행의 prevHash 를 여기서 얻는다. */
  Optional<AuditLogEntity> findTopByOrderByChainSeqDesc();

  /** 체인 검증용 순차 조회. */
  List<AuditLogEntity> findByChainSeqBetweenOrderByChainSeqAsc(Long from, Long to);

  Page<AuditLogEntity> findByActorIdOrderByAuditIdDesc(Long actorId, Pageable pageable);

  /** 해시 체인이 적용되기 전에 쌓인 행 수 (검증 시작점 계산용). */
  long countByChainSeqIsNull();
}
