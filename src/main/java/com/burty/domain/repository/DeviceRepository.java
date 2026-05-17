package com.burty.domain.repository;

import com.burty.domain.entity.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<DeviceEntity, UUID> {
    List<DeviceEntity> findByUser_UserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(Long userId);
    Optional<DeviceEntity> findByUser_UserIdAndDeviceFingerprintAndRevokedAtIsNull(Long userId, String deviceFingerprint);
    Optional<DeviceEntity> findByDeviceTokenHashAndRevokedAtIsNull(String deviceTokenHash);
}
