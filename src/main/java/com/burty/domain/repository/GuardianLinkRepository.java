package com.burty.domain.repository;

import com.burty.domain.entity.GuardianLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuardianLinkRepository extends JpaRepository<GuardianLinkEntity, Long> {
    List<GuardianLinkEntity> findBySeniorUser_UserId(Long seniorUserId);
}
