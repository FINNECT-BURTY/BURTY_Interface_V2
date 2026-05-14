package com.berty.domain.repository;

import com.berty.domain.entity.LinkedInstitutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinkedInstitutionRepository extends JpaRepository<LinkedInstitutionEntity, UUID> {
    List<LinkedInstitutionEntity> findByUser_UserId(UUID userId);

    Optional<LinkedInstitutionEntity> findByUser_UserIdAndInstitutionCode(UUID userId, String institutionCode);
}
