package com.berty.adapter.in.web;

import com.berty.adapter.in.web.dto.DeviceNameUpdateRequest;
import com.berty.adapter.in.web.dto.DeviceResponse;
import com.berty.adapter.in.web.dto.SimpleResultResponse;
import com.berty.core.dto.response.ApiResponse;
import com.berty.domain.entity.BiometricCredentialEntity;
import com.berty.domain.entity.DeviceEntity;
import com.berty.domain.repository.BiometricCredentialRepository;
import com.berty.domain.repository.DeviceRepository;
import com.berty.security.AuthLevel;
import com.berty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/berty/devices")
@Tag(name = "BERTY Device Management", description = "신뢰 디바이스 조회/이름 변경/해제 API")
public class DeviceManagementController {
    private final DeviceRepository deviceRepository;
    private final BiometricCredentialRepository biometricCredentialRepository;

    public DeviceManagementController(DeviceRepository deviceRepository,
                                      BiometricCredentialRepository biometricCredentialRepository) {
        this.deviceRepository = deviceRepository;
        this.biometricCredentialRepository = biometricCredentialRepository;
    }

    @GetMapping
    @AuthLevel(RiskLevel.LEVEL_2)
    @Operation(summary = "등록 기기 목록", description = "사용자에게 등록된 신뢰 기기 목록을 조회합니다.")
    public ApiResponse<List<DeviceResponse>> devices(@RequestParam String userId) {
        UUID userUuid = UUID.fromString(userId);
        List<DeviceResponse> responses = deviceRepository.findByUser_UserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(userUuid)
                .stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.ok(responses);
    }

    @PatchMapping("/{deviceId}/name")
    @AuthLevel(RiskLevel.LEVEL_2)
    @Operation(summary = "기기명 변경", description = "등록된 기기의 표시 이름을 변경합니다.")
    public ApiResponse<DeviceResponse> updateName(@PathVariable String deviceId, @RequestBody DeviceNameUpdateRequest request) {
        DeviceEntity device = deviceRepository.findById(UUID.fromString(deviceId)).orElseThrow();
        assertOwner(device, request.getUserId());
        device.setDeviceName(request.getDeviceName());
        device.setUpdatedAt(LocalDateTime.now());
        return ApiResponse.ok(toResponse(deviceRepository.save(device)));
    }

    @DeleteMapping("/{deviceId}")
    @AuthLevel(RiskLevel.LEVEL_3)
    @Operation(summary = "기기 해제", description = "분실/교체된 신뢰 기기를 해제하고 연결된 생체 credential도 폐기합니다.")
    public ApiResponse<SimpleResultResponse> revoke(@PathVariable String deviceId, @RequestParam String userId) {
        UUID deviceUuid = UUID.fromString(deviceId);
        DeviceEntity device = deviceRepository.findById(deviceUuid).orElseThrow();
        assertOwner(device, userId);
        LocalDateTime now = LocalDateTime.now();
        device.setIsTrusted(false);
        device.setRevokedAt(now);
        device.setUpdatedAt(now);
        deviceRepository.save(device);

        for (BiometricCredentialEntity credential : biometricCredentialRepository.findByDevice_DeviceIdAndRevokedAtIsNull(deviceUuid)) {
            credential.setRevokedAt(now);
            biometricCredentialRepository.save(credential);
        }
        return ApiResponse.ok(new SimpleResultResponse(true, "기기가 해제되었습니다."));
    }

    private DeviceResponse toResponse(DeviceEntity device) {
        return new DeviceResponse(
                device.getDeviceId().toString(),
                device.getDeviceName(),
                device.getPlatform() == null ? null : device.getPlatform().name(),
                device.getOsVersion(),
                device.getAppVersion(),
                Boolean.TRUE.equals(device.getIsTrusted()),
                device.getLastSeenAt(),
                device.getCreatedAt()
        );
    }

    private void assertOwner(DeviceEntity device, String userId) {
        if (device.getUser() == null || !device.getUser().getUserId().toString().equals(userId)) {
            throw new IllegalArgumentException("device owner mismatch");
        }
    }
}
