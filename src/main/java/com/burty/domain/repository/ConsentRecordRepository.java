package com.burty.domain.repository;

import com.burty.domain.entity.ConsentRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsentRecordRepository extends JpaRepository<ConsentRecordEntity, UUID> {
    List<ConsentRecordEntity> findByUser_UserId(UUID userId);
    List<ConsentRecordEntity> findByUser_UserIdOrderByAgreedAtDesc(UUID userId);
}
