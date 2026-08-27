package com.burty.domain.user.repository;

import com.burty.domain.user.entity.DataErasureRequestEntity;
import com.burty.domain.user.entity.DataErasureRequestEntity.Status;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataErasureRequestRepository
    extends JpaRepository<DataErasureRequestEntity, Long> {

  Optional<DataErasureRequestEntity> findFirstByUserIdOrderByErasureIdDesc(Long userId);

  /** 법정 보존기간이 끝나 잔여 데이터를 파기해야 하는 건. */
  List<DataErasureRequestEntity> findByStatusAndRetentionUntilLessThanEqualOrderByErasureIdAsc(
      Status status, LocalDateTime now);
}
