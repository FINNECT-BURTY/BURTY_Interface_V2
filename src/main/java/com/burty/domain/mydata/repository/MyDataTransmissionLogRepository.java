package com.burty.domain.mydata.repository;

import com.burty.domain.mydata.entity.MyDataTransmissionLogEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MyDataTransmissionLogRepository
    extends JpaRepository<MyDataTransmissionLogEntity, Long> {

  List<MyDataTransmissionLogEntity> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
      String userId, LocalDateTime since);

  List<MyDataTransmissionLogEntity> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime since);
}
