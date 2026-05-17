package com.burty.domain.repository;

import com.burty.domain.entity.GuardianLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GuardianLinkRepository extends JpaRepository<GuardianLinkEntity, UUID> {
    List<GuardianLinkEntity> findBySeniorUser_UserId(Long seniorUserId);
}
