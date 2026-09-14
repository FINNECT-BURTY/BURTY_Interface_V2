/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 애플리케이션 서비스 (UserProfileService)</b>
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

import com.burty.application.dto.user.UserSettingsResponse;
import com.burty.application.dto.user.UserSettingsUpdateRequest;
import com.burty.application.port.in.user.UserProfileUseCase;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.user.entity.UserProfileEntity;
import com.burty.domain.user.repository.UserProfileRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService implements UserProfileUseCase {

  /**
   * 글자 배율의 상·하한.
   *
   * <p>아래로는 기본 크기보다 작게 만들지 않는다 — 시니어 대상 서비스에서 글자를 줄이는 설정은 실수로 눌렀을 때 되돌리기가 가장 어렵다. 위로는 화면이 무너지지 않는
   * 선까지만 연다. 컬럼도 {@code precision 3, scale 2} 라 소수 두 자리까지만 담는다.
   */
  private static final BigDecimal MIN_FONT_SCALE = new BigDecimal("1.00");

  private static final BigDecimal MAX_FONT_SCALE = new BigDecimal("1.50");

  private final UserProfileRepository userProfileRepository;

  public UserProfileService(UserProfileRepository userProfileRepository) {
    this.userProfileRepository = userProfileRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public String getUserName(Long userId) {
    return findProfile(userId).getName();
  }

  @Override
  @Transactional(readOnly = true)
  public UserSettingsResponse getSettings(Long userId) {
    return toResponse(findProfile(userId));
  }

  @Override
  @Transactional
  public UserSettingsResponse updateSettings(Long userId, UserSettingsUpdateRequest request) {
    if (request == null || (blank(request.uxMode()) && request.fontScale() == null)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "바꿀 설정을 입력해 주세요.");
    }

    UserProfileEntity profile = findProfile(userId);
    if (!blank(request.uxMode())) {
      profile.setUxMode(parseUxMode(request.uxMode()));
    }
    if (request.fontScale() != null) {
      profile.setFontScale(normalizeFontScale(request.fontScale()));
    }
    profile.setUpdatedAt(LocalDateTime.now());
    return toResponse(userProfileRepository.save(profile));
  }

  private UserProfileEntity findProfile(Long userId) {
    return userProfileRepository
        .findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "사용자 프로필을 찾을 수 없습니다."));
  }

  private static UserSettingsResponse toResponse(UserProfileEntity profile) {
    BigDecimal fontScale = profile.getFontScale() == null ? MIN_FONT_SCALE : profile.getFontScale();
    return new UserSettingsResponse(
        profile.getUxMode() == null
            ? UserProfileEntity.UxMode.STANDARD.name()
            : profile.getUxMode().name(),
        fontScale,
        Boolean.TRUE.equals(profile.getVoiceEnabled()));
  }

  private static UserProfileEntity.UxMode parseUxMode(String raw) {
    try {
      return UserProfileEntity.UxMode.valueOf(raw.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE, "uxMode는 SENIOR 또는 STANDARD 여야 합니다.");
    }
  }

  /**
   * 배율을 컬럼이 담을 수 있는 형태로 맞춘다.
   *
   * <p>범위를 벗어난 값은 자르지 않고 거절한다. 조용히 잘라 저장하면 화면은 사용자가 고른 값이 반영된 줄 알고, 다음에 열었을 때 다른 값이 보인다.
   */
  private static BigDecimal normalizeFontScale(BigDecimal raw) {
    if (raw.compareTo(MIN_FONT_SCALE) < 0 || raw.compareTo(MAX_FONT_SCALE) > 0) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE,
          "글자 배율은 %s 이상 %s 이하여야 합니다.".formatted(MIN_FONT_SCALE, MAX_FONT_SCALE));
    }
    return raw.setScale(2, RoundingMode.HALF_UP);
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
