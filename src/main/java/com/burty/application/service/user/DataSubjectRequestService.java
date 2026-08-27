package com.burty.application.service.user;

import com.burty.application.service.support.AuditLogger;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.auth.repository.SocialAccountRepository;
import com.burty.domain.finance.repository.TransferOrderRepository;
import com.burty.domain.mydata.repository.LinkedInstitutionRepository;
import com.burty.domain.mydata.repository.MyDataConsentHistoryRepository;
import com.burty.domain.transaction.repository.TransactionRepository;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.entity.UserProfileEntity;
import com.burty.domain.user.repository.DataErasureRequestRepository;
import com.burty.domain.user.repository.DeviceRepository;
import com.burty.domain.user.repository.UserProfileRepository;
import com.burty.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 정보주체 권리 행사 (Data Subject Request).
 *
 * <p>개인정보보호법상 정보주체는 자신의 정보에 대해 <b>열람·정정·처리정지·파기</b>를 요구할 수 있다. 마이데이터 사업자라면 이 창구가 API 로 있어야 한다. 기존
 * 코드베이스에는 이 개념 자체가 없었다.
 *
 * <p>여기서는 열람(내 정보 전체 내보내기)과 정정을 담당한다. 파기는 {@link UserWithdrawalService} 가, 처리정지(마이데이터 수집 중단)는
 * {@code MyDataConsentEnforcementService} 가 담당한다.
 */
@Service
public class DataSubjectRequestService {

  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;
  private final DeviceRepository deviceRepository;
  private final SocialAccountRepository socialAccountRepository;
  private final LinkedInstitutionRepository linkedInstitutionRepository;
  private final MyDataConsentHistoryRepository consentHistoryRepository;
  private final TransactionRepository transactionRepository;
  private final TransferOrderRepository transferOrderRepository;
  private final DataErasureRequestRepository erasureRepository;
  private final AuditLogger auditLogger;

  public DataSubjectRequestService(
      UserRepository userRepository,
      UserProfileRepository userProfileRepository,
      DeviceRepository deviceRepository,
      SocialAccountRepository socialAccountRepository,
      LinkedInstitutionRepository linkedInstitutionRepository,
      MyDataConsentHistoryRepository consentHistoryRepository,
      TransactionRepository transactionRepository,
      TransferOrderRepository transferOrderRepository,
      DataErasureRequestRepository erasureRepository,
      AuditLogger auditLogger) {
    this.userRepository = userRepository;
    this.userProfileRepository = userProfileRepository;
    this.deviceRepository = deviceRepository;
    this.socialAccountRepository = socialAccountRepository;
    this.linkedInstitutionRepository = linkedInstitutionRepository;
    this.consentHistoryRepository = consentHistoryRepository;
    this.transactionRepository = transactionRepository;
    this.transferOrderRepository = transferOrderRepository;
    this.erasureRepository = erasureRepository;
    this.auditLogger = auditLogger;
  }

  /**
   * 열람권 — 보유 중인 내 정보 전체를 구조화해 반환한다.
   *
   * <p>"어떤 정보를 갖고 있는지" 를 정보주체가 확인할 수 있어야 한다. 개별 조회 API 를 여러 개 호출해서 짜맞추는 것과는 다르다. 여기서는 카테고리별 보유 사실과
   * 건수를 함께 제공해, 사용자가 무엇이 수집됐는지 한눈에 보게 한다.
   */
  @Transactional(readOnly = true)
  public Map<String, Object> exportPersonalData(String userId) {
    long numericUserId = parseUserId(userId);
    UserEntity user =
        userRepository
            .findById(numericUserId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

    Map<String, Object> export = new LinkedHashMap<>();
    export.put("exportedAt", LocalDateTime.now());
    export.put("userId", numericUserId);

    Map<String, Object> account = new LinkedHashMap<>();
    account.put("status", user.getStatus().name());
    account.put("phone", user.getPhone());
    account.put("createdAt", user.getCreatedAt());
    account.put("lastLoginAt", user.getLastLoginAt());
    account.put("withdrawnAt", user.getWithdrawnAt());
    export.put("account", account);

    userProfileRepository
        .findById(numericUserId)
        .ifPresent(
            profile -> {
              Map<String, Object> p = new LinkedHashMap<>();
              p.put("name", profile.getName());
              p.put("birthdate", profile.getBirthdate());
              p.put("uxMode", String.valueOf(profile.getUxMode()));
              export.put("profile", p);
            });

    export.put(
        "linkedInstitutions",
        linkedInstitutionRepository.findByUser_UserId(numericUserId).stream()
            .<Map<String, Object>>map(
                link ->
                    Map.of(
                        "institutionCode", String.valueOf(link.getInstitutionCode()),
                        "institutionName", String.valueOf(link.getInstitutionName()),
                        "status", String.valueOf(link.getStatus()),
                        "consentExpiresAt", String.valueOf(link.getConsentExpiresAt()),
                        "lastSyncedAt", String.valueOf(link.getLastSyncedAt())))
            .toList());

    export.put(
        "consentHistory",
        consentHistoryRepository.findByUserIdOrderByAgreedAtDesc(userId).stream()
            .<Map<String, Object>>map(
                c ->
                    Map.of(
                        "institutionCode", String.valueOf(c.getInstitutionCode()),
                        "scope", String.valueOf(c.getScope()),
                        "agreedAt", String.valueOf(c.getAgreedAt()),
                        "revokedAt", String.valueOf(c.getRevokedAt())))
            .toList());

    export.put(
        "devices",
        deviceRepository
            .findByUser_UserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(numericUserId)
            .stream()
            .<Map<String, Object>>map(
                d ->
                    Map.of(
                        "platform", String.valueOf(d.getPlatform()),
                        "lastSeenAt", String.valueOf(d.getLastSeenAt())))
            .toList());

    export.put(
        "socialAccounts",
        socialAccountRepository.findByUserId(numericUserId).stream()
            .<Map<String, Object>>map(a -> Map.of("provider", String.valueOf(a.getProvider())))
            .toList());

    // 거래·이체는 건수가 많을 수 있어 요약만 제공하고, 상세는 기존 조회 API(페이지)를 안내한다.
    Map<String, Object> counts = new LinkedHashMap<>();
    counts.put(
        "transactions", transactionRepository.findByUserIdOrderByTxnDateDesc(numericUserId).size());
    counts.put("transferOrders", transferOrderRepository.findByUser_UserId(numericUserId).size());
    export.put("financialRecordCounts", counts);
    export.put(
        "financialRecordsDetailEndpoint",
        List.of("GET /api/v1/transactions?page=0&size=50", "GET /api/v1/transfers"));

    erasureRepository
        .findFirstByUserIdOrderByErasureIdDesc(numericUserId)
        .ifPresent(
            e ->
                export.put(
                    "erasure",
                    Map.of(
                        "status", e.getStatus().name(),
                        "requestedAt", String.valueOf(e.getRequestedAt()),
                        "retentionUntil", String.valueOf(e.getRetentionUntil()),
                        "note", "전자금융거래 기록은 법정 보존의무에 따라 보존기간까지 유지된 뒤 파기됩니다.")));

    auditLogger.logSuccess(userId, "DSR_EXPORT", String.valueOf(numericUserId), "개인정보 열람 요청 처리");
    return export;
  }

  /**
   * 정정권 — 정정 가능한 항목만 허용한다.
   *
   * <p>이름·전화번호 같은 본인확인 기반 항목은 임의 수정 대상이 아니다 (본인확인 절차를 다시 거쳐야 한다). 여기서는 사용자가 직접 입력한 표시용 항목만 정정한다.
   */
  @Transactional
  public void rectify(String userId, String field, String value) {
    long numericUserId = parseUserId(userId);
    UserProfileEntity profile =
        userProfileRepository
            .findById(numericUserId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "프로필을 찾을 수 없습니다."));

    switch (field == null ? "" : field.toLowerCase()) {
      case "name" -> {
        if (value == null || value.isBlank() || value.length() > 100) {
          throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이름은 1~100자여야 합니다.");
        }
        profile.setName(value.trim());
      }
      default ->
          throw new BusinessException(
              ErrorCode.OPERATION_NOT_ALLOWED,
              "정정할 수 없는 항목입니다. 전화번호·생년월일 등 본인확인 기반 정보는 본인확인 절차를 통해 변경해야 합니다.");
    }
    userProfileRepository.save(profile);
    auditLogger.logSuccess(userId, "DSR_RECTIFY", field, "개인정보 정정 요청 처리");
  }

  private static long parseUserId(String userId) {
    try {
      return Long.parseLong(userId);
    } catch (NumberFormatException e) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 사용자 ID입니다.");
    }
  }
}
