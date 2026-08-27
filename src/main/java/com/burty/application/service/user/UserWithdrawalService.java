package com.burty.application.service.user;

import com.burty.application.service.mydata.MyDataConsentEnforcementService;
import com.burty.application.service.support.AuditLogger;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.auth.repository.BiometricCredentialRepository;
import com.burty.domain.auth.repository.SocialAccountRepository;
import com.burty.domain.auth.repository.UserSessionRepository;
import com.burty.domain.mydata.entity.LinkedInstitutionEntity;
import com.burty.domain.mydata.repository.LinkedInstitutionRepository;
import com.burty.domain.user.entity.DataErasureRequestEntity;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.entity.UserProfileEntity;
import com.burty.domain.user.repository.DataErasureRequestRepository;
import com.burty.domain.user.repository.DeviceRepository;
import com.burty.domain.user.repository.PersonaProfileRepository;
import com.burty.domain.user.repository.UserProfileRepository;
import com.burty.domain.user.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴 및 개인정보 파기.
 *
 * <p>기존에는 {@code UserStatus.WITHDRAWN} 과 {@code withdrawnAt} 컬럼만 있고 <b>이를 사용하는 코드가 없었다.</b> 즉 탈퇴 기능
 * 자체가 없었고, 48개 엔티티에 흩어진 개인정보를 어떻게 처리할지에 대한 정의도 없었다.
 *
 * <p>두 단계로 나눈다. 이 구분이 핵심이다.
 *
 * <ol>
 *   <li><b>즉시 처리</b> — 직접 식별정보(CI, 전화번호, 이름, 생년월일)를 익명화하고, 인증 수단(세션·기기·생체인증·소셜연동)을 폐기하며, 마이데이터로 수집한
 *       계좌 데이터를 파기한다. 로그인이 즉시 불가능해진다.
 *   <li><b>보존 후 파기</b> — 전자금융거래 기록과 감사 로그는 법정 보존의무가 있어 즉시 지울 수 없다. 보존기간이 지나면 {@code
 *       PersonalDataRetentionBatch} 가 잔여 데이터를 파기한다.
 * </ol>
 *
 * <p>"전부 즉시 삭제" 는 규제 위반이고, "아무것도 안 지움" 도 위반이다. 어느 쪽인지 코드가 명시적으로 말해야 한다.
 */
@Service
public class UserWithdrawalService {

  private static final Logger log = LoggerFactory.getLogger(UserWithdrawalService.class);
  private static final SecureRandom RANDOM = new SecureRandom();

  /** 익명화된 생년월일 sentinel. 시간대 변환에 안전한 값이어야 한다 (아래 주석 참고). */
  private static final LocalDate ANONYMIZED_BIRTHDATE = LocalDate.of(1970, 1, 1);

  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;
  private final PersonaProfileRepository personaProfileRepository;
  private final DeviceRepository deviceRepository;
  private final BiometricCredentialRepository biometricCredentialRepository;
  private final SocialAccountRepository socialAccountRepository;
  private final UserSessionRepository userSessionRepository;
  private final LinkedInstitutionRepository linkedInstitutionRepository;
  private final MyDataConsentEnforcementService consentEnforcementService;
  private final DataErasureRequestRepository erasureRepository;
  private final AuditLogger auditLogger;
  private final Clock clock;
  private final int retentionYears;

  public UserWithdrawalService(
      UserRepository userRepository,
      UserProfileRepository userProfileRepository,
      PersonaProfileRepository personaProfileRepository,
      DeviceRepository deviceRepository,
      BiometricCredentialRepository biometricCredentialRepository,
      SocialAccountRepository socialAccountRepository,
      UserSessionRepository userSessionRepository,
      LinkedInstitutionRepository linkedInstitutionRepository,
      MyDataConsentEnforcementService consentEnforcementService,
      DataErasureRequestRepository erasureRepository,
      AuditLogger auditLogger,
      Clock clock,
      @Value("${burty.privacy.transaction-retention-years:5}") int retentionYears) {
    this.userRepository = userRepository;
    this.userProfileRepository = userProfileRepository;
    this.personaProfileRepository = personaProfileRepository;
    this.deviceRepository = deviceRepository;
    this.biometricCredentialRepository = biometricCredentialRepository;
    this.socialAccountRepository = socialAccountRepository;
    this.userSessionRepository = userSessionRepository;
    this.linkedInstitutionRepository = linkedInstitutionRepository;
    this.consentEnforcementService = consentEnforcementService;
    this.erasureRepository = erasureRepository;
    this.auditLogger = auditLogger;
    this.clock = clock;
    this.retentionYears = retentionYears;
  }

  /**
   * 탈퇴 처리.
   *
   * @return 처리 내역 요약 (증빙용)
   */
  @Transactional
  public DataErasureRequestEntity withdraw(String userId, String reason) {
    long numericUserId = parseUserId(userId);
    UserEntity user =
        userRepository
            .findById(numericUserId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

    if (user.getStatus() == UserEntity.UserStatus.WITHDRAWN) {
      throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "이미 탈퇴한 계정입니다.");
    }

    LocalDateTime now = LocalDateTime.now(clock);
    Map<String, Integer> summary = new LinkedHashMap<>();

    // 1) 마이데이터 연동 전부 해지 + 수집 데이터 파기
    summary.put("purgedAccounts", revokeAllInstitutions(userId, numericUserId));

    // 2) 인증 수단 폐기 — 즉시 로그인 불가 상태로 만든다.
    summary.put("revokedSessions", revokeSessions(numericUserId, now));
    summary.put("revokedDevices", revokeDevices(numericUserId, now));
    summary.put("revokedCredentials", revokeBiometrics(numericUserId, now));
    summary.put("deletedSocialLinks", deleteSocialAccounts(numericUserId));

    // 3) 직접 식별정보 익명화
    anonymizeProfile(numericUserId, summary);
    anonymizeUser(user, now);

    // 4) 파기 요청 기록 (증빙 + 잔여 데이터 파기 예약)
    DataErasureRequestEntity erasure = new DataErasureRequestEntity();
    erasure.setUserId(numericUserId);
    erasure.setReason(DataErasureRequestEntity.Reason.WITHDRAWAL);
    erasure.setStatus(DataErasureRequestEntity.Status.IMMEDIATE_DONE);
    erasure.setRequestedAt(now);
    erasure.setAnonymizedAt(now);
    erasure.setRetentionUntil(now.plusYears(retentionYears));
    erasure.setSummary(summary.toString());
    erasureRepository.save(erasure);

    auditLogger.logSuccess(
        userId,
        "USER_WITHDRAWAL",
        String.valueOf(numericUserId),
        "reason=%s, %s".formatted(reason, summary));
    log.info("회원 탈퇴 처리 완료 userId={} reason={} summary={}", userId, reason, summary);
    return erasure;
  }

  private int revokeAllInstitutions(String userId, long numericUserId) {
    List<LinkedInstitutionEntity> links =
        linkedInstitutionRepository.findByUser_UserId(numericUserId);
    int purged = 0;
    for (LinkedInstitutionEntity link : links) {
      purged +=
          consentEnforcementService.enforceRevocation(
              userId, link.getInstitutionCode(), "USER_WITHDRAWAL", true);
    }
    return purged;
  }

  private int revokeSessions(long userId, LocalDateTime now) {
    var sessions = userSessionRepository.findByUserIdAndRevokedAtIsNull(userId);
    sessions.forEach(s -> s.setRevokedAt(now));
    userSessionRepository.saveAll(sessions);
    return sessions.size();
  }

  private int revokeDevices(long userId, LocalDateTime now) {
    var devices = deviceRepository.findByUser_UserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(userId);
    devices.forEach(d -> d.setRevokedAt(now));
    deviceRepository.saveAll(devices);
    return devices.size();
  }

  private int revokeBiometrics(long userId, LocalDateTime now) {
    var credentials = biometricCredentialRepository.findByUser_UserIdAndRevokedAtIsNull(userId);
    credentials.forEach(c -> c.setRevokedAt(now));
    biometricCredentialRepository.saveAll(credentials);
    return credentials.size();
  }

  private int deleteSocialAccounts(long userId) {
    var accounts = socialAccountRepository.findByUserId(userId);
    socialAccountRepository.deleteAll(accounts);
    return accounts.size();
  }

  private void anonymizeProfile(long userId, Map<String, Integer> summary) {
    UserProfileEntity profile = userProfileRepository.findById(userId).orElse(null);
    if (profile != null) {
      profile.setName("탈퇴회원");
      // 생년월일은 NOT NULL 이라 삭제할 수 없다. 식별력이 없는 고정값으로 대체한다.
      //
      // 1900-01-01 을 쓰지 않는 이유: 한국은 1908년 이전 UTC 오프셋이 +08:27:52 라서
      // JDBC 의 LocalDate ↔ java.sql.Date 변환에서 하루가 밀린다 (1899-12-31 로 저장됨).
      // 에포크는 모든 시간대에서 안전하게 왕복된다.
      profile.setBirthdate(ANONYMIZED_BIRTHDATE);
      userProfileRepository.save(profile);
      summary.put("anonymizedProfiles", 1);
    }
    personaProfileRepository
        .findByUserId(userId)
        .ifPresent(
            persona -> {
              personaProfileRepository.delete(persona);
              summary.put("deletedPersonas", 1);
            });
  }

  /**
   * 사용자 레코드 익명화.
   *
   * <p>{@code ci_hash} / {@code phone_hash} 는 UNIQUE 제약이 있어 NULL 이나 고정값으로 바꿀 수 없다 (여러 명이 탈퇴하면 충돌).
   * 되돌릴 수 없는 난수로 덮어써서 식별력을 없애면서 제약도 만족시킨다.
   */
  private void anonymizeUser(UserEntity user, LocalDateTime now) {
    String nonce = randomHex();
    user.setCi("WITHDRAWN");
    user.setCiHash(nonce);
    user.setPhone("000-0000-0000");
    user.setPhoneHash(randomHex());
    user.setStatus(UserEntity.UserStatus.WITHDRAWN);
    user.setWithdrawnAt(now);
    user.setLastLoginIp(null);
    user.setUpdatedAt(now);
    userRepository.save(user);
  }

  private static String randomHex() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  private static long parseUserId(String userId) {
    try {
      return Long.parseLong(userId);
    } catch (NumberFormatException e) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 사용자 ID입니다.");
    }
  }
}
