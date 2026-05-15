package com.burty.domain.repository;

import com.burty.domain.entity.FamilyConsentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FamilyConsentRepository extends JpaRepository<FamilyConsentEntity, String> {
    List<FamilyConsentEntity> findByParentUserId(String parentUserId);

    Optional<FamilyConsentEntity> findByParentUserIdAndChildUserId(String parentUserId, String childUserId);
}
