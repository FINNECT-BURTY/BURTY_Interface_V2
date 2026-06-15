package com.burty.domain.mydata.repository;

import com.burty.domain.mydata.entity.MyDataConsentHistoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MyDataConsentHistoryRepository
    extends JpaRepository<MyDataConsentHistoryEntity, Long> {

  List<MyDataConsentHistoryEntity> findByUserIdOrderByAgreedAtDesc(String userId);

  List<MyDataConsentHistoryEntity> findByUserIdAndInstitutionCodeOrderByAgreedAtDesc(
      String userId, String institutionCode);
}
