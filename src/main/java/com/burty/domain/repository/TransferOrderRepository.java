package com.burty.domain.repository;

import com.burty.domain.entity.TransferOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransferOrderRepository extends JpaRepository<TransferOrderEntity, UUID> {
    List<TransferOrderEntity> findByUser_UserId(Long userId);

    Optional<TransferOrderEntity> findByIdempotencyKey(String idempotencyKey);
}
