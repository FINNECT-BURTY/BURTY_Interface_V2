package com.burty.domain.repository;

import com.burty.domain.entity.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface DeviceRepository extends JpaRepository<DeviceEntity, UUID> {
    List<DeviceEntity> findByUser_UserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(UUID userId);
    Optional<DeviceEntity> findByUser_UserIdAndDeviceFingerprintAndRevokedAtIsNull(UUID userId, String deviceFingerprint);
    Optional<DeviceEntity> findByDeviceTokenHashAndRevokedAtIsNull(String deviceTokenHash);
}
