package com.burty.domain.repository;

import com.burty.domain.entity.LinkedInstitutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LinkedInstitutionRepository extends JpaRepository<LinkedInstitutionEntity, Long> {
    List<LinkedInstitutionEntity> findByUser_UserId(Long userId);

    Optional<LinkedInstitutionEntity> findByUser_UserIdAndInstitutionCode(Long userId, String institutionCode);
}
