/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 애플리케이션 서비스 (DeviceManagementService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.user
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.application.service.user;

import com.burty.adapter.in.web.mapper.WebResponseMapper;
import com.burty.application.dto.user.DeviceNameUpdateRequest;
import com.burty.application.dto.user.DeviceResponse;
import com.burty.application.port.in.user.DeviceManagementUseCase;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.auth.entity.BiometricCredentialEntity;
import com.burty.domain.auth.repository.BiometricCredentialRepository;
import com.burty.domain.user.entity.DeviceEntity;
import com.burty.domain.user.repository.DeviceRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceManagementService implements DeviceManagementUseCase {

  private final DeviceRepository deviceRepository;
  private final BiometricCredentialRepository biometricCredentialRepository;
  private final WebResponseMapper webResponseMapper;

  @Override
  @Transactional(readOnly = true)
  public List<DeviceResponse> listDevices(String userId) {
    return deviceRepository
        .findByUser_UserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(Long.parseLong(userId))
        .stream()
        .map(webResponseMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public DeviceResponse updateDeviceName(String deviceId, DeviceNameUpdateRequest request) {
    DeviceEntity device =
        deviceRepository
            .findById(Long.parseLong(deviceId))
            .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "기기를 찾을 수 없습니다."));
    assertOwner(device, request.userId());
    device.setDeviceName(request.deviceName());
    device.setUpdatedAt(LocalDateTime.now());
    return webResponseMapper.toResponse(deviceRepository.save(device));
  }

  @Override
  @Transactional
  public void revokeDevice(String deviceId, String userId) {
    Long deviceKey = Long.parseLong(deviceId);
    DeviceEntity device =
        deviceRepository
            .findById(deviceKey)
            .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "기기를 찾을 수 없습니다."));
    assertOwner(device, userId);
    LocalDateTime now = LocalDateTime.now();
    device.setIsTrusted(false);
    device.setRevokedAt(now);
    device.setUpdatedAt(now);
    deviceRepository.save(device);

    for (BiometricCredentialEntity credential :
        biometricCredentialRepository.findByDevice_DeviceIdAndRevokedAtIsNull(deviceKey)) {
      credential.setRevokedAt(now);
      biometricCredentialRepository.save(credential);
    }
  }

  private void assertOwner(DeviceEntity device, String userId) {
    if (device.getUser() == null || !device.getUser().getUserId().toString().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "기기 소유자가 일치하지 않습니다.");
    }
  }
}
