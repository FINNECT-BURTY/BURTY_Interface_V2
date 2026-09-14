package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.burty.adapter.in.web.mapper.WebResponseMapper;
import com.burty.application.dto.user.DeviceFcmTokenRequest;
import com.burty.application.service.user.DeviceManagementService;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.auth.repository.BiometricCredentialRepository;
import com.burty.domain.user.entity.DeviceEntity;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.DeviceRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 푸시 등록 토큰.
 *
 * <p>기기에 {@code fcm_token} 컬럼과 그것을 읽는 발송 경로는 있었지만 값을 넣는 곳이 없었다. 그래서 푸시 대상은 늘 비어 있었고 알림은 한 건도 가지
 * 않았다.
 */
class DeviceFcmTokenTests {

  private static final String OWNER = "7";
  private static final String DEVICE_ID = "11";

  private DeviceRepository devices;
  private BiometricCredentialRepository credentials;
  private DeviceManagementService service;
  private DeviceEntity device;

  @BeforeEach
  void setUp() {
    UserEntity owner = new UserEntity();
    owner.setUserId(7L);

    device = new DeviceEntity();
    device.setDeviceId(11L);
    device.setUser(owner);
    device.setCreatedAt(LocalDateTime.now());
    device.setUpdatedAt(LocalDateTime.now());

    devices = mock(DeviceRepository.class);
    credentials = mock(BiometricCredentialRepository.class);
    when(devices.findById(11L)).thenReturn(Optional.of(device));
    when(devices.save(any(DeviceEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(credentials.findByDevice_DeviceIdAndRevokedAtIsNull(anyLong())).thenReturn(List.of());

    service = new DeviceManagementService(devices, credentials, mock(WebResponseMapper.class));
  }

  private DeviceEntity savedDevice() {
    ArgumentCaptor<DeviceEntity> captor = ArgumentCaptor.forClass(DeviceEntity.class);
    verify(devices).save(captor.capture());
    return captor.getValue();
  }

  @Test
  @DisplayName("토큰을 기기에 저장한다")
  void storesToken() {
    service.registerFcmToken(DEVICE_ID, OWNER, new DeviceFcmTokenRequest("fcm-abc"));

    assertEquals("fcm-abc", savedDevice().getFcmToken());
  }

  @Test
  @DisplayName("앱이 다시 실행되면 갱신된 토큰으로 덮어쓴다")
  void overwritesToken() {
    device.setFcmToken("fcm-old");

    service.registerFcmToken(DEVICE_ID, OWNER, new DeviceFcmTokenRequest("fcm-new"));

    assertEquals("fcm-new", savedDevice().getFcmToken());
  }

  @Test
  @DisplayName("다른 사용자의 기기에는 등록하지 못한다")
  void rejectsNonOwner() {
    // 남의 기기에 내 주소를 넣을 수 있으면 그 사용자의 알림이 나에게 온다.
    BusinessException e =
        assertThrows(
            BusinessException.class,
            () -> service.registerFcmToken(DEVICE_ID, "8", new DeviceFcmTokenRequest("fcm-abc")));

    assertEquals(ErrorCode.FORBIDDEN, e.getErrorCode());
    verify(devices, never()).save(any(DeviceEntity.class));
  }

  @Test
  @DisplayName("해제된 기기에는 등록하지 못한다")
  void rejectsRevokedDevice() {
    device.setRevokedAt(LocalDateTime.now());

    BusinessException e =
        assertThrows(
            BusinessException.class,
            () -> service.registerFcmToken(DEVICE_ID, OWNER, new DeviceFcmTokenRequest("fcm-abc")));

    assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, e.getErrorCode());
    verify(devices, never()).save(any(DeviceEntity.class));
  }

  @Test
  @DisplayName("기기를 해제하면 푸시 주소도 지운다")
  void clearsTokenOnRevoke() {
    device.setFcmToken("fcm-abc");

    service.revokeDevice(DEVICE_ID, OWNER);

    assertNull(savedDevice().getFcmToken());
  }
}
