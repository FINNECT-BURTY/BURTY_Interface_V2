package com.nuri.domain.repository;

import com.nuri.domain.entity.BiometricCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BiometricCredentialRepository extends JpaRepository<BiometricCredentialEntity, UUID> {
    Optional<BiometricCredentialEntity> findFirstByUser_UserIdAndRevokedAtIsNull(UUID userId);
}
