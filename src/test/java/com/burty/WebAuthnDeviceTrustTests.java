package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.burty.adapter.out.security.WebAuthnDeviceTrustManager;
import com.burty.domain.user.entity.DeviceEntity;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.DeviceRepository;
import com.burty.domain.user.repository.UserRepository;
import com.burty.util.AccountNumberHasher;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 신뢰 기기 등록·조회 동작을 못 박는다.
 *
 * <p>기기 토큰은 한 번 신뢰한 기기를 다시 묻지 않고 통과시키는 값이다. 지문이 다르면 다른 기기여야 하고, 남의 토큰으로는 조회되지 않아야 한다. 이 패키지에는 참조
 * 테스트가 하나도 없었다.
 */
class WebAuthnDeviceTrustTests {

  private static final Long USER_KEY = 1L;

  private DeviceRepository devices;
  private WebAuthnDeviceTrustManager manager;

  @BeforeEach
  void setUp() {
    UserEntity user = new UserEntity();
    user.setUserId(USER_KEY);

    UserRepository users = mock(UserRepository.class);
    when(users.findById(anyLong())).thenReturn(Optional.of(user));

    devices = mock(DeviceRepository.class);
    when(devices.findByUser_UserIdAndDeviceFingerprintAndRevokedAtIsNull(anyLong(), anyString()))
        .thenReturn(Optional.empty());
    // save 는 받은 것을 그대로 돌려준다. 저장 계층을 흉내내는 것이 목적이 아니다.
    when(devices.save(any(DeviceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

    manager = new WebAuthnDeviceTrustManager(users, devices, new AccountNumberHasher());
  }

  @Test
  @DisplayName("새 기기는 토큰을 발급받고 신뢰 상태가 된다")
  void newDeviceGetsToken() {
    var pair = manager.ensureTrustedDevice(USER_KEY, "fp-a", "IOS", null);

    assertNotNull(pair.plainToken());
    assertTrue(pair.plainToken().startsWith("bdt_"));
    assertEquals(Boolean.TRUE, pair.device().getIsTrusted());
  }

  @Test
  @DisplayName("지문이 다르면 다른 기기다")
  void differentFingerprintIsDifferentDevice() {
    // 지문을 무시하고 같은 기기로 묶으면, 한 번 신뢰한 사용자의 다른 기기가
    // 전부 무조건 통과하게 된다.
    var first = manager.ensureTrustedDevice(USER_KEY, "fp-a", "WEB", null);
    var second = manager.ensureTrustedDevice(USER_KEY, "fp-b", "WEB", null);

    assertNotEquals(first.device().getDeviceFingerprint(), second.device().getDeviceFingerprint());
  }

  @Test
  @DisplayName("지문은 해시로 저장한다 — 원문을 남기지 않는다")
  void fingerprintIsHashed() {
    var pair = manager.ensureTrustedDevice(USER_KEY, "fp-plain", "WEB", null);

    assertNotEquals("fp-plain", pair.device().getDeviceFingerprint());
    assertEquals(new AccountNumberHasher().hash("fp-plain"), pair.device().getDeviceFingerprint());
  }

  @Test
  @DisplayName("모르는 플랫폼 문자열은 WEB 으로 떨어뜨린다")
  void unknownPlatformFallsBackToWeb() {
    // 클라이언트가 보내는 값이라 신뢰할 수 없다. 예외로 등록 자체를 막으면
    // 새 플랫폼이 생길 때마다 인증이 끊긴다.
    var pair = manager.ensureTrustedDevice(USER_KEY, "fp-a", "SMART_FRIDGE", null);

    assertEquals(DeviceEntity.Platform.WEB, pair.device().getPlatform());
  }

  @Test
  @DisplayName("토큰이 없으면 기기를 찾지 않는다")
  void blankTokenFindsNothing() {
    assertNull(manager.findDeviceByToken(USER_KEY, null));
    assertNull(manager.findDeviceByToken(USER_KEY, "  "));
    assertNull(manager.findDeviceByToken(null, "bdt_something"));
  }

  @Test
  @DisplayName("남의 기기 토큰으로는 조회되지 않는다")
  void tokenOfAnotherUserIsRejected() {
    // 토큰 해시만 맞으면 통과시키면, 토큰이 유출됐을 때 소유자 확인 없이 쓰인다.
    UserEntity other = new UserEntity();
    other.setUserId(999L);
    DeviceEntity otherDevice = new DeviceEntity();
    otherDevice.setUser(other);

    when(devices.findByDeviceTokenHashAndRevokedAtIsNull(anyString()))
        .thenReturn(Optional.of(otherDevice));

    assertNull(manager.findDeviceByToken(USER_KEY, "bdt_stolen"));
  }

  @Test
  @DisplayName("기기 토큰은 매번 다르다")
  void deviceTokensAreUnique() {
    // 예측 가능한 토큰이면 남의 기기를 신뢰 상태로 만들 수 있다.
    var first = manager.ensureTrustedDevice(USER_KEY, "fp-a", "WEB", null);
    var second = manager.ensureTrustedDevice(USER_KEY, "fp-a", "WEB", null);

    assertNotEquals(first.plainToken(), second.plainToken());
  }
}
