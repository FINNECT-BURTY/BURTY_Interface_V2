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

import com.burty.application.dto.auth.SignupConsents;
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
import java.net.InetAddress;
import java.net.UnknownHostException;
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
      SignupConsents consents,
      String ipAddress,
      String userAgent) {
    if (consents == null || !consents.terms()) {
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

    persistSignupConsents(user, now, consents, ipAddress, userAgent);

    auditLogger.logSuccess(userId, "COMPLETE_ONBOARDING", userId, "profile=true");
    return new OnboardingProfileResult(true, false);
  }

  /**
   * 항목별 동의 기록.
   *
   * <p>예전에는 화면이 여섯 항목에 동의를 받아도 필수 약관 두 건만 남겼다. 수집·이용, 전송요구, 마케팅, 혜택 동의는 기록이 없어 무엇에 동의했는지 증명할 수 없었다.
   * 선택 동의는 특히 문제였다 — 기록이 없으면 수신 거부 사용자에게 보내지 않았음을 보일 수 없다.
   *
   * <p>동의하지 않은 항목은 남기지 않는다. 거부를 "동의함" 으로 적으면 기록이 거짓이 된다.
   *
   * <p>개인신용정보·마케팅 문서는 개인정보 문서와 같은 버전으로 관리한다. 문서 버전이 갈라지면 그때 설정을 나눈다.
   */
  private void persistSignupConsents(
      UserEntity user,
      LocalDateTime agreedAt,
      SignupConsents consents,
      String ipAddress,
      String userAgent) {
    String termsVersion = onboardingProperties.getTermsVersion();
    String privacyVersion = onboardingProperties.getPrivacyVersion();

    persistConsent(
        user, ConsentRecordEntity.ConsentType.TERMS, termsVersion, agreedAt, ipAddress, userAgent);
    if (consents.privacy()) {
      persistConsent(
          user,
          ConsentRecordEntity.ConsentType.PRIVACY,
          privacyVersion,
          agreedAt,
          ipAddress,
          userAgent);
    }
    if (consents.creditCollection()) {
      persistConsent(
          user,
          ConsentRecordEntity.ConsentType.CREDIT_COLLECTION,
          privacyVersion,
          agreedAt,
          ipAddress,
          userAgent);
    }
    if (consents.creditTransfer()) {
      persistConsent(
          user,
          ConsentRecordEntity.ConsentType.MYDATA,
          privacyVersion,
          agreedAt,
          ipAddress,
          userAgent);
    }
    if (consents.marketing()) {
      persistConsent(
          user,
          ConsentRecordEntity.ConsentType.MARKETING,
          privacyVersion,
          agreedAt,
          ipAddress,
          userAgent);
    }
    if (consents.benefit()) {
      persistConsent(
          user,
          ConsentRecordEntity.ConsentType.BENEFIT_NOTICE,
          privacyVersion,
          agreedAt,
          ipAddress,
          userAgent);
    }
    // AI 상담·음성은 국외 사업자를 거친다. 이 기록이 없으면 운영에서 그 두 기능이 막힌다 (#171).
    if (consents.overseasTransfer()) {
      persistConsent(
          user,
          ConsentRecordEntity.ConsentType.THIRD_PARTY_SHARE,
          privacyVersion,
          agreedAt,
          ipAddress,
          userAgent);
    }
  }

  private void persistConsent(
      UserEntity user,
      ConsentRecordEntity.ConsentType type,
      String version,
      LocalDateTime agreedAt,
      String ipAddress,
      String userAgent) {
    ConsentRecordEntity c = new ConsentRecordEntity();
    c.setUser(user);
    c.setConsentType(type);
    c.setConsentVersion(version);
    c.setDocumentHash(accountNumberHasher.hash(type.name() + "|v=" + version));
    c.setAgreedAt(agreedAt);
    c.setIpAddress(toIpBytes(ipAddress));
    c.setUserAgent(truncate(userAgent, 255));
    consentRecordRepository.save(c);
  }

  /**
   * 동의 시점의 접속 IP.
   *
   * <p>이름을 받아 DNS 를 타지 않도록 IP 형태만 받는다. 확인할 수 없으면 비워 둔다 — 틀린 IP 를 남기는 것보다 없는 편이 낫다.
   */
  private static byte[] toIpBytes(String ipAddress) {
    if (ipAddress == null || ipAddress.isBlank() || "unknown".equals(ipAddress)) {
      return null;
    }
    if (!ipAddress.matches("[0-9a-fA-F.:]+")) {
      return null;
    }
    try {
      return InetAddress.getByName(ipAddress).getAddress();
    } catch (UnknownHostException e) {
      return null;
    }
  }

  private static String truncate(String value, int max) {
    if (value == null) {
      return null;
    }
    return value.length() <= max ? value : value.substring(0, max);
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
