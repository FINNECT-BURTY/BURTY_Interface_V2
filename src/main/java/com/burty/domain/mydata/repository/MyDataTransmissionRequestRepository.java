package com.burty.domain.mydata.repository;

import com.burty.domain.mydata.entity.MyDataTransmissionRequestEntity;
import com.burty.domain.mydata.entity.MyDataTransmissionRequestEntity.Status;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MyDataTransmissionRequestRepository
    extends JpaRepository<MyDataTransmissionRequestEntity, Long> {

  List<MyDataTransmissionRequestEntity> findByUserIdOrderByRequestedAtDesc(String userId);

  Optional<MyDataTransmissionRequestEntity>
      findFirstByUserIdAndInstitutionCodeAndStatusOrderByRequestedAtDesc(
          String userId, String institutionCode, Status status);
}
