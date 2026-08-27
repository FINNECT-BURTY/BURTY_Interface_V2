package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.application.service.user.DataSubjectRequestService;
import com.burty.application.service.user.UserWithdrawalService;
import com.burty.core.exception.BusinessException;
import com.burty.domain.auth.entity.SocialAccountEntity;
import com.burty.domain.auth.repository.SocialAccountRepository;
import com.burty.domain.user.entity.DataErasureRequestEntity;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.entity.UserEntity.UserStatus;
import com.burty.domain.user.entity.UserProfileEntity;
import com.burty.domain.user.repository.DataErasureRequestRepository;
import com.burty.domain.user.repository.UserProfileRepository;
import com.burty.domain.user.repository.UserRepository;
import com.burty.support.IntegrationTestBase;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 회원 탈퇴 및 개인정보 파기 검증.
 *
 * <p>이 경로는 <b>되돌릴 수 없다.</b> 너무 적게 지우면 개인정보보호법 위반이고, 너무 많이 지우면 전자금융거래법상 보존의무 위반이다. 어느 쪽으로도 틀리면 안
 * 되므로, "무엇이 지워지고 무엇이 남는지" 를 테스트로 못박는다.
 */
@SpringBootTest
class UserWithdrawalTests extends IntegrationTestBase {

  @Autowired private UserWithdrawalService withdrawalService;
  @Autowired private DataSubjectRequestService dataSubjectRequestService;
  @Autowired private UserRepository userRepository;
  @Autowired private UserProfileRepository userProfileRepository;
  @Autowired private SocialAccountRepository socialAccountRepository;
  @Autowired private DataErasureRequestRepository erasureRepository;
  @Autowired private TransactionTemplate transactionTemplate;

  private long userId;
  private String originalCiHash;
  private String originalPhoneHash;

  @BeforeEach
  void setUp() {
    UserEntity user = createUserWithProfile();
    userId = user.getUserId();
    originalCiHash = user.getCiHash();
    originalPhoneHash = user.getPhoneHash();
  }

  // ── 즉시 파기되어야 하는 것 ──────────────────────────────────────────────────

  @Test
  @DisplayName("탈퇴하면 직접 식별정보(CI·전화번호)가 복구 불가능하게 익명화된다")
  void directIdentifiersAreAnonymized() {
    withdrawalService.withdraw(String.valueOf(userId), "테스트");

    UserEntity user = reloadUser();
    assertEquals(UserStatus.WITHDRAWN, user.getStatus());
    assertNotNull(user.getWithdrawnAt());
    assertEquals("WITHDRAWN", user.getCi(), "CI 원본이 남아 있으면 안 된다");
    assertNotEquals(originalCiHash, user.getCiHash(), "CI 해시로도 동일인 식별이 가능하면 안 된다");
    assertNotEquals(originalPhoneHash, user.getPhoneHash());
    assertFalse(user.getPhone().contains("1234"), "전화번호 원본이 남아 있으면 안 된다");
  }

  @Test
  @DisplayName("프로필의 이름·생년월일이 식별력 없는 값으로 대체된다")
  void profileIsAnonymized() {
    withdrawalService.withdraw(String.valueOf(userId), "테스트");

    UserProfileEntity profile =
        transactionTemplate.execute(s -> userProfileRepository.findById(userId).orElseThrow());
    assertEquals("탈퇴회원", profile.getName());
    // sentinel 은 시간대 변환에 안전한 값이어야 한다. 1900-01-01 은 한국의 1908년 이전
    // UTC 오프셋(+08:27:52) 때문에 1899-12-31 로 밀린다.
    assertEquals(LocalDate.of(1970, 1, 1), profile.getBirthdate());
    assertNotEquals(LocalDate.of(1955, 3, 14), profile.getBirthdate(), "원본 생년월일이 남으면 안 된다");
  }

  @Test
  @DisplayName("소셜 연동은 삭제되어 재로그인이 불가능해진다")
  void socialAccountsAreDeleted() {
    transactionTemplate.execute(
        s -> {
          SocialAccountEntity social = new SocialAccountEntity();
          social.setUserId(userId);
          social.setProvider("kakao");
          social.setProviderUserIdHash(UUID.randomUUID().toString());
          return socialAccountRepository.save(social);
        });
    assertEquals(1, socialAccountRepository.findByUserId(userId).size());

    withdrawalService.withdraw(String.valueOf(userId), "테스트");

    assertTrue(socialAccountRepository.findByUserId(userId).isEmpty(), "소셜 연동이 남으면 탈퇴 후에도 로그인이 된다");
  }

  // ── 보존되어야 하는 것 ──────────────────────────────────────────────────────

  @Test
  @DisplayName("파기 요청 기록이 남아 처리 증빙과 잔여 보존기간을 확인할 수 있다")
  void erasureRecordProvesProcessingAndRetention() {
    DataErasureRequestEntity erasure = withdrawalService.withdraw(String.valueOf(userId), "테스트");

    assertEquals(DataErasureRequestEntity.Reason.WITHDRAWAL, erasure.getReason());
    assertEquals(DataErasureRequestEntity.Status.IMMEDIATE_DONE, erasure.getStatus());
    assertNotNull(erasure.getAnonymizedAt());
    assertNotNull(erasure.getSummary(), "무엇을 지웠는지 증빙이 남아야 한다");
    assertTrue(
        erasure.getRetentionUntil().isAfter(LocalDateTime.now().plusYears(4)),
        "전자금융거래 기록은 법정 보존기간(기본 5년) 동안 유지되어야 한다");

    assertTrue(erasureRepository.findFirstByUserIdOrderByErasureIdDesc(userId).isPresent());
  }

  @Test
  @DisplayName("사용자 레코드 자체는 남는다 (거래 기록의 참조 무결성 유지)")
  void userRowSurvivesForReferentialIntegrity() {
    withdrawalService.withdraw(String.valueOf(userId), "테스트");

    assertTrue(userRepository.findById(userId).isPresent(), "사용자 행을 지우면 보존해야 할 거래 기록의 FK 가 깨진다");
  }

  // ── 멱등성·방어 ───────────────────────────────────────────────────────────

  @Test
  @DisplayName("이미 탈퇴한 계정은 다시 탈퇴할 수 없다")
  void doubleWithdrawalIsRejected() {
    withdrawalService.withdraw(String.valueOf(userId), "테스트");

    assertThrows(
        BusinessException.class, () -> withdrawalService.withdraw(String.valueOf(userId), "재시도"));
  }

  @Test
  @DisplayName("존재하지 않는 사용자의 탈퇴는 거부된다")
  void withdrawingUnknownUserFails() {
    assertThrows(BusinessException.class, () -> withdrawalService.withdraw("99999999", "테스트"));
  }

  // ── 정보주체 열람권 ─────────────────────────────────────────────────────────

  @Test
  @DisplayName("열람 요청은 보유 중인 정보를 카테고리별로 반환하고 보존 예정도 알린다")
  void exportReturnsHeldDataAndRetentionNotice() {
    Map<String, Object> before =
        dataSubjectRequestService.exportPersonalData(String.valueOf(userId));
    assertNotNull(before.get("account"));
    assertNotNull(before.get("profile"));
    assertNotNull(before.get("financialRecordCounts"));

    withdrawalService.withdraw(String.valueOf(userId), "테스트");

    Map<String, Object> after =
        dataSubjectRequestService.exportPersonalData(String.valueOf(userId));
    @SuppressWarnings("unchecked")
    Map<String, Object> erasure = (Map<String, Object>) after.get("erasure");
    assertNotNull(erasure, "탈퇴 후에는 파기 처리 현황이 함께 조회되어야 한다");
    assertEquals("IMMEDIATE_DONE", erasure.get("status"));
    assertTrue(String.valueOf(erasure.get("note")).contains("법정 보존"));
  }

  // ── 헬퍼 ─────────────────────────────────────────────────────────────────

  private UserEntity createUserWithProfile() {
    return transactionTemplate.execute(
        s -> {
          String nonce = UUID.randomUUID().toString().replace("-", "");
          UserEntity user = new UserEntity();
          user.setCiHash(nonce + nonce);
          user.setCi("ci-original-" + nonce);
          user.setPhoneHash(nonce);
          user.setPhone("010-1234-5678");
          user.setCreatedAt(LocalDateTime.now());
          user.setUpdatedAt(LocalDateTime.now());
          UserEntity saved = userRepository.save(user);

          UserProfileEntity profile = new UserProfileEntity();
          profile.setUser(saved);
          profile.setName("홍길동");
          profile.setBirthdate(LocalDate.of(1955, 3, 14));
          profile.setCreatedAt(LocalDateTime.now());
          profile.setUpdatedAt(LocalDateTime.now());
          userProfileRepository.save(profile);
          return saved;
        });
  }

  private UserEntity reloadUser() {
    return transactionTemplate.execute(s -> userRepository.findById(userId).orElseThrow());
  }
}
