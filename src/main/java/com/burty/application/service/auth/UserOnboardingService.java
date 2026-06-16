/**
 *
 *
 * <pre>
 * <b>Description  : 인증 애플리케이션 서비스 (UserOnboardingService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.auth
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
package com.burty.application.service.auth;

import com.burty.application.port.in.auth.UserOnboardingUseCase;
import com.burty.application.service.support.AuditLogger;
import com.burty.config.BurtyOnboardingProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.auth.model.OnboardingProfileResult;
import com.burty.domain.user.entity.ConsentRecordEntity;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.entity.UserProfileEntity;
import com.burty.domain.user.repository.ConsentRecordRepository;
import com.burty.domain.user.repository.UserProfileRepository;
import com.burty.domain.user.repository.UserRepository;
import com.burty.util.AccountNumberHasher;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserOnboardingService implements UserOnboardingUseCase {

  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;
  private final ConsentRecordRepository consentRecordRepository;
  private final BurtyOnboardingProperties onboardingProperties;
  private final AccountNumberHasher accountNumberHasher;
  private final AuditLogger auditLogger;

  public UserOnboardingService(
      UserRepository userRepository,
      UserProfileRepository userProfileRepository,
      ConsentRecordRepository consentRecordRepository,
      BurtyOnboardingProperties onboardingProperties,
      AccountNumberHasher accountNumberHasher,
      AuditLogger auditLogger) {
    this.userRepository = userRepository;
    this.userProfileRepository = userProfileRepository;
    this.consentRecordRepository = consentRecordRepository;
    this.onboardingProperties = onboardingProperties;
    this.accountNumberHasher = accountNumberHasher;
    this.auditLogger = auditLogger;
  }

  @Override
  @Transactional
  public OnboardingProfileResult completeProfile(
      String userId,
      String phone,
      String name,
      LocalDate birthDate,
      Integer ageRange,
      String uxModeRaw,
      boolean termsAccepted) {
    if (!termsAccepted) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "필수 약관(LGN-006)에 동의해야 합니다.");
    }
    if (blank(name)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "실명을 입력해 주세요.");
    }
    if (birthDate == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "생년월일을 입력해 주세요.");
    }
    if (birthDate.isAfter(LocalDate.now())) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "생년월일이 올바르지 않습니다.");
    }
    if (Period.between(birthDate, LocalDate.now()).getYears() > 120) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "생년월일이 올바르지 않습니다.");
    }

    Long numericUserId;
    try {
      numericUserId = Long.parseLong(userId);
    } catch (NumberFormatException e) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "userId 형식이 올바르지 않습니다.");
    }

    if (userProfileRepository.existsById(numericUserId)) {
      return new OnboardingProfileResult(true, true);
    }

    String normalizedPhone = normalizeKoreanMobile(phone);
    String phoneHash = accountNumberHasher.hash(normalizedPhone);
    userRepository
        .findByPhoneHash(phoneHash)
        .filter(u -> !u.getUserId().equals(numericUserId))
        .ifPresent(
            u -> {
              throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "이미 사용 중인 휴대폰 번호입니다.");
            });

    UserEntity user =
        userRepository
            .findById(numericUserId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

    user.setPhoneHash(phoneHash);
    user.setPhone(normalizedPhone);
    user.setUpdatedAt(LocalDateTime.now());
    userRepository.save(user);

    UserProfileEntity profile = new UserProfileEntity();
    profile.setUser(user);
    profile.setName(name.trim());
    profile.setBirthdate(birthDate);
    profile.setAgeRange(ageRange);
    profile.setUxMode(parseUxMode(uxModeRaw));
    profile.setFontScale(BigDecimal.ONE);
    profile.setVoiceEnabled(false);
    LocalDateTime now = LocalDateTime.now();
    profile.setCreatedAt(now);
    profile.setUpdatedAt(now);
    userProfileRepository.save(profile);

    persistSignupConsents(user, now);

    auditLogger.logSuccess(userId, "COMPLETE_ONBOARDING", userId, "profile=true");
    return new OnboardingProfileResult(true, false);
  }

  private void persistSignupConsents(UserEntity user, LocalDateTime agreedAt) {
    persistConsent(
        user,
        ConsentRecordEntity.ConsentType.TERMS,
        onboardingProperties.getTermsVersion(),
        agreedAt);
    persistConsent(
        user,
        ConsentRecordEntity.ConsentType.PRIVACY,
        onboardingProperties.getPrivacyVersion(),
        agreedAt);
  }

  private void persistConsent(
      UserEntity user,
      ConsentRecordEntity.ConsentType type,
      String version,
      LocalDateTime agreedAt) {
    ConsentRecordEntity c = new ConsentRecordEntity();
    c.setUser(user);
    c.setConsentType(type);
    c.setConsentVersion(version);
    c.setDocumentHash(accountNumberHasher.hash(type.name() + "|v=" + version));
    c.setAgreedAt(agreedAt);
    consentRecordRepository.save(c);
  }

  private UserProfileEntity.UxMode parseUxMode(String raw) {
    if (blank(raw)) {
      return UserProfileEntity.UxMode.STANDARD;
    }
    try {
      return UserProfileEntity.UxMode.valueOf(raw.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE, "uxMode는 SENIOR 또는 STANDARD 여야 합니다.");
    }
  }

  private String normalizeKoreanMobile(String raw) {
    if (blank(raw)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "휴대폰 번호를 입력해 주세요.");
    }
    String digits = raw.replaceAll("\\D", "");
    if (digits.startsWith("82")) {
      digits = "0" + digits.substring(2);
    }
    if (digits.length() < 10 || digits.length() > 11 || !digits.startsWith("0")) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "휴대폰 번호 형식이 올바르지 않습니다.");
    }
    return digits;
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
