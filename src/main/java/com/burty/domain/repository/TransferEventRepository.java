package com.burty.domain.repository;

import com.burty.domain.entity.TransferEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransferEventRepository extends JpaRepository<TransferEventEntity, Long> {
    List<TransferEventEntity> findByOrder_OrderIdOrderBySequenceNoAsc(UUID orderId);
}
