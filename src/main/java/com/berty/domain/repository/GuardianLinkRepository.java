package com.berty.domain.repository;

import com.berty.domain.entity.GuardianLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GuardianLinkRepository extends JpaRepository<GuardianLinkEntity, UUID> {
    List<GuardianLinkEntity> findBySeniorUser_UserId(UUID seniorUserId);
}
