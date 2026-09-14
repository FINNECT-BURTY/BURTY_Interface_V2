package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.burty.application.dto.user.UserSettingsResponse;
import com.burty.application.dto.user.UserSettingsUpdateRequest;
import com.burty.application.service.user.UserProfileService;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.user.entity.UserProfileEntity;
import com.burty.domain.user.repository.UserProfileRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 표시 설정.
 *
 * <p>가입할 때부터 {@code ux_mode} 와 {@code font_scale} 을 저장했지만 읽거나 바꿀 방법이 없어 값이 늘 기본값에 머물렀다. 시니어를 대상으로
 * 하는 서비스에서 글자 크기를 바꿀 수 없다는 뜻이었다 (#134).
 */
class UserDisplaySettingsTests {

  private static final Long USER_ID = 7L;

  private UserProfileRepository profiles;
  private UserProfileService service;
  private UserProfileEntity profile;

  @BeforeEach
  void setUp() {
    profile = new UserProfileEntity();
    profile.setUserId(USER_ID);
    profile.setName("홍길동");
    profile.setUxMode(UserProfileEntity.UxMode.STANDARD);
    profile.setFontScale(new BigDecimal("1.00"));
    profile.setVoiceEnabled(false);

    profiles = mock(UserProfileRepository.class);
    when(profiles.findById(USER_ID)).thenReturn(Optional.of(profile));
    when(profiles.save(any(UserProfileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    service = new UserProfileService(profiles);
  }

  @Test
  @DisplayName("저장된 설정을 그대로 돌려준다")
  void returnsStoredSettings() {
    profile.setUxMode(UserProfileEntity.UxMode.SENIOR);
    profile.setFontScale(new BigDecimal("1.20"));

    UserSettingsResponse response = service.getSettings(USER_ID);

    assertEquals("SENIOR", response.uxMode());
    assertEquals(0, response.fontScale().compareTo(new BigDecimal("1.20")));
  }

  @Test
  @DisplayName("프로필이 없으면 찾을 수 없다고 알린다")
  void rejectsMissingProfile() {
    when(profiles.findById(USER_ID)).thenReturn(Optional.empty());

    BusinessException e = assertThrows(BusinessException.class, () -> service.getSettings(USER_ID));

    assertEquals(ErrorCode.USER_NOT_FOUND, e.getErrorCode());
  }

  @Test
  @DisplayName("글자 배율만 바꾸면 모드는 그대로 둔다")
  void updatesOnlyFontScale() {
    // 화면에서 건드리지 않은 설정이 조용히 되돌아가면 안 된다.
    profile.setUxMode(UserProfileEntity.UxMode.SENIOR);

    UserSettingsResponse response =
        service.updateSettings(USER_ID, new UserSettingsUpdateRequest(null, new BigDecimal("1.4")));

    assertEquals("SENIOR", response.uxMode());
    assertEquals(0, response.fontScale().compareTo(new BigDecimal("1.40")));
  }

  @Test
  @DisplayName("모드만 바꾸면 글자 배율은 그대로 둔다")
  void updatesOnlyUxMode() {
    profile.setFontScale(new BigDecimal("1.20"));

    UserSettingsResponse response =
        service.updateSettings(USER_ID, new UserSettingsUpdateRequest("senior", null));

    assertEquals("SENIOR", response.uxMode());
    assertEquals(0, response.fontScale().compareTo(new BigDecimal("1.20")));
  }

  @Test
  @DisplayName("범위를 벗어난 배율은 잘라 저장하지 않고 거절한다")
  void rejectsOutOfRangeFontScale() {
    // 조용히 잘라 저장하면 화면은 고른 값이 반영된 줄 알고, 다음에 열었을 때 다른 값이 보인다.
    BusinessException tooLarge =
        assertThrows(
            BusinessException.class,
            () ->
                service.updateSettings(
                    USER_ID, new UserSettingsUpdateRequest(null, new BigDecimal("3.0"))));
    BusinessException tooSmall =
        assertThrows(
            BusinessException.class,
            () ->
                service.updateSettings(
                    USER_ID, new UserSettingsUpdateRequest(null, new BigDecimal("0.5"))));

    assertEquals(ErrorCode.INVALID_INPUT_VALUE, tooLarge.getErrorCode());
    assertEquals(ErrorCode.INVALID_INPUT_VALUE, tooSmall.getErrorCode());
    verify(profiles, never()).save(any(UserProfileEntity.class));
  }

  @Test
  @DisplayName("알 수 없는 모드는 거절한다")
  void rejectsUnknownUxMode() {
    BusinessException e =
        assertThrows(
            BusinessException.class,
            () -> service.updateSettings(USER_ID, new UserSettingsUpdateRequest("LARGE", null)));

    assertEquals(ErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
    verify(profiles, never()).save(any(UserProfileEntity.class));
  }

  @Test
  @DisplayName("바꿀 값이 하나도 없으면 거절한다")
  void rejectsEmptyUpdate() {
    BusinessException e =
        assertThrows(
            BusinessException.class,
            () -> service.updateSettings(USER_ID, new UserSettingsUpdateRequest(null, null)));

    assertEquals(ErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
    verify(profiles, never()).save(any(UserProfileEntity.class));
  }

  @Test
  @DisplayName("컬럼이 담는 소수 두 자리로 맞춘다")
  void normalizesScale() {
    UserSettingsResponse response =
        service.updateSettings(
            USER_ID, new UserSettingsUpdateRequest(null, new BigDecimal("1.234")));

    assertEquals(2, response.fontScale().scale());
    assertEquals(0, response.fontScale().compareTo(new BigDecimal("1.23")));
  }
}
