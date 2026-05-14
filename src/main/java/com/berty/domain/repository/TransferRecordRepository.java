package com.berty.domain.repository;

import com.berty.domain.entity.TransferRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferRecordRepository extends JpaRepository<TransferRecordEntity, String> {
    List<TransferRecordEntity> findByUserIdOrderByCreatedAtDesc(String userId);
}
