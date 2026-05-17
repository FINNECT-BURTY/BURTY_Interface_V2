package com.burty.domain.repository;

import com.burty.domain.entity.ConsentRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsentRecordRepository extends JpaRepository<ConsentRecordEntity, Long> {
    List<ConsentRecordEntity> findByUser_UserId(Long userId);
    List<ConsentRecordEntity> findByUser_UserIdOrderByAgreedAtDesc(Long userId);
}
