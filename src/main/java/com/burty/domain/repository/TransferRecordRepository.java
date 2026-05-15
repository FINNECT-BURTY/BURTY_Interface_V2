package com.burty.domain.repository;

import com.burty.domain.entity.TransferRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferRecordRepository extends JpaRepository<TransferRecordEntity, String> {
    List<TransferRecordEntity> findByUserIdOrderByCreatedAtDesc(String userId);
}
