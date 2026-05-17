package com.burty.domain.repository;

import com.burty.domain.entity.BiometricCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BiometricCredentialRepository extends JpaRepository<BiometricCredentialEntity, Long> {
    Optional<BiometricCredentialEntity> findFirstByUser_UserIdAndRevokedAtIsNull(Long userId);
    List<BiometricCredentialEntity> findByUser_UserIdAndRevokedAtIsNull(Long userId);
    List<BiometricCredentialEntity> findByDevice_DeviceIdAndRevokedAtIsNull(Long deviceId);
}
