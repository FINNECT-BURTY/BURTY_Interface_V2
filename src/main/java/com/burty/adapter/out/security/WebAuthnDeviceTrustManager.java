/**
 *
 *
 * <pre>
 * <b>Description  : 보안 (WebAuthnDeviceTrustManager)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.out.security
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
package com.burty.adapter.out.security;

import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.user.entity.DeviceEntity;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.DeviceRepository;
import com.burty.domain.user.repository.UserRepository;
import com.burty.util.AccountNumberHasher;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class WebAuthnDeviceTrustManager {

  private final UserRepository userRepository;
  private final DeviceRepository deviceRepository;
  private final AccountNumberHasher accountNumberHasher;

  public WebAuthnDeviceTrustManager(
      UserRepository userRepository,
      DeviceRepository deviceRepository,
      AccountNumberHasher accountNumberHasher) {
    this.userRepository = userRepository;
    this.deviceRepository = deviceRepository;
    this.accountNumberHasher = accountNumberHasher;
  }

  public DeviceTokenPair ensureTrustedDevice(
      Long userKey, String deviceFingerprint, String platform, String existingToken) {
    UserEntity user =
        userRepository
            .findById(userKey)
            .orElseThrow(
                () -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다."));
    String fingerprint = normalizeFingerprint(deviceFingerprint, userKey);
    DeviceEntity device =
        deviceRepository
            .findByUser_UserIdAndDeviceFingerprintAndRevokedAtIsNull(userKey, fingerprint)
            .orElseGet(DeviceEntity::new);
    String plainToken = existingToken;
    if (device.getDeviceId() == null) {
      plainToken = blank(plainToken) ? issueDeviceToken(userKey, fingerprint) : plainToken;
      device.setDeviceTokenHash(accountNumberHasher.hash(plainToken));
      device.setDeviceToken(plainToken);
      device.setCreatedAt(LocalDateTime.now());
    } else if (blank(plainToken)) {
      plainToken = device.getDeviceToken();
    }
    device.setUser(user);
    device.setDeviceFingerprint(fingerprint);
    device.setPlatform(parsePlatform(platform));
    device.setIsTrusted(true);
    device.setLastSeenAt(LocalDateTime.now());
    device.setUpdatedAt(LocalDateTime.now());
    return new DeviceTokenPair(deviceRepository.save(device), plainToken);
  }

  public DeviceEntity defaultDevice(Long userKey) {
    return ensureTrustedDevice(userKey, "default-fingerprint-" + userKey, "WEB", null).device();
  }

  public DeviceEntity findDeviceByToken(Long userKey, String deviceToken) {
    if (userKey == null || blank(deviceToken)) {
      return null;
    }
    return deviceRepository
        .findByDeviceTokenHashAndRevokedAtIsNull(accountNumberHasher.hash(deviceToken))
        .filter(device -> device.getUser() != null && userKey.equals(device.getUser().getUserId()))
        .orElse(null);
  }

  public void touchDevice(DeviceEntity device) {
    device.setLastSeenAt(LocalDateTime.now());
    device.setUpdatedAt(LocalDateTime.now());
    deviceRepository.save(device);
  }

  private String issueDeviceToken(Long userKey, String fingerprint) {
    return "bdt_"
        + Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                (userKey + ":" + fingerprint + ":" + UUID.randomUUID())
                    .getBytes(StandardCharsets.UTF_8));
  }

  private String normalizeFingerprint(String deviceFingerprint, Long userKey) {
    if (!blank(deviceFingerprint)) {
      return accountNumberHasher.hash(deviceFingerprint);
    }
    return accountNumberHasher.hash("default-fingerprint-" + userKey);
  }

  private DeviceEntity.Platform parsePlatform(String platform) {
    if (blank(platform)) {
      return DeviceEntity.Platform.WEB;
    }
    try {
      return DeviceEntity.Platform.valueOf(platform.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return DeviceEntity.Platform.WEB;
    }
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }

  public record DeviceTokenPair(DeviceEntity device, String plainToken) {}
}
