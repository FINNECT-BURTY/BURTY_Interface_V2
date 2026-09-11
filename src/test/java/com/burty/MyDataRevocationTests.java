package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.application.port.in.mydata.MyDataAuthUseCase;
import com.burty.application.port.out.mydata.MyDataOAuthPort;
import com.burty.application.service.batch.MyDataTokenRefreshBatch;
import com.burty.application.service.mydata.LinkedInstitutionPersistenceService;
import com.burty.application.service.mydata.MyDataConsentEnforcementService;
import com.burty.domain.mydata.entity.LinkedInstitutionEntity;
import com.burty.domain.mydata.entity.LinkedInstitutionEntity.LinkStatus;
import com.burty.domain.mydata.repository.LinkedInstitutionRepository;
import com.burty.domain.mydata.repository.MyDataLinkStatusRepository;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.UserRepository;
import com.burty.support.IntegrationTestBase;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 전송요구 철회가 실제로 수집을 멈추는지 확인한다.
 *
 * <p>철회·동의 만료·탈퇴는 모두 {@code enforceRevocation} 을 탄다. 그런데 그것이 DB 에 있는 토큰만 무효화하고 실제로 쓰이는 런타임
 * 저장소(Redis)는 그대로 두었다. 자산 조회도, 토큰 갱신 배치도 런타임 저장소를 읽으므로 철회가 서류상으로만 끝났다.
 *
 * <p>더 나빴던 것은 갱신 배치다. 링크 상태 테이블은 철회 후에도 ACTIVE 로 남아 배치가 그 연동을 골랐고, 남아 있던 리프레시 토큰으로 새 토큰을 받아 {@code
 * saveTokens} 로 저장했다. 그 메서드가 상태를 ACTIVE 로 되돌리고 동의 만료일을 1년 뒤로 다시 썼다 — 철회가 15분 안에 되돌려졌다.
 *
 * <p>stub 모드에서도 갱신 경로는 똑같이 돌기 때문에 여기서 재현된다.
 */
@SpringBootTest
class MyDataRevocationTests extends IntegrationTestBase {

  private static final String INSTITUTION = "KB";

  @Autowired private UserRepository userRepository;
  @Autowired private MyDataAuthUseCase authUseCase;
  @Autowired private MyDataConsentEnforcementService enforcement;
  @Autowired private MyDataTokenRefreshBatch refreshBatch;
  @Autowired private MyDataOAuthPort oauthPort;
  @Autowired private LinkedInstitutionRepository linkedInstitutionRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private LinkedInstitutionPersistenceService linkedInstitutionPersistence;
  @Autowired private MyDataLinkStatusRepository linkStatusRepository;

  private String userId;
  private String scopeKey;

  @BeforeEach
  void linkInstitution() {
    userId = createUser();
    assertTrue(authUseCase.exchangeAuthorizationCode(userId, INSTITUTION, "auth-code"));
    scopeKey = MyDataOAuthPort.scopeKey(userId, INSTITUTION);

    // 연결 직후 런타임 토큰이 있어야 아래 "남지 않는다" 검사가 의미를 갖는다.
    assertNotNull(oauthPort.findAccessToken(scopeKey), "연결 직후 런타임 access token 이 없다");
    assertNotNull(oauthPort.findRefreshToken(scopeKey), "연결 직후 런타임 refresh token 이 없다");
  }

  @Test
  @DisplayName("철회하면 런타임 저장소에 토큰이 남지 않는다")
  void revocationClearsRuntimeTokens() {
    enforcement.enforceRevocation(userId, INSTITUTION, "TEST_REVOKE", false);

    assertNull(oauthPort.findAccessToken(scopeKey), "철회 후에도 access token 이 남아 수집에 쓰인다");
    assertNull(oauthPort.findRefreshToken(scopeKey), "철회 후에도 refresh token 이 남아 재발급에 쓰인다");
  }

  @Test
  @DisplayName("철회한 연동을 토큰 갱신 배치가 되살리지 않는다")
  void refreshBatchDoesNotResurrectRevokedLink() {
    enforcement.enforceRevocation(userId, INSTITUTION, "TEST_REVOKE", false);

    runRefreshBatch();

    assertEquals(LinkStatus.REVOKED, link().getStatus(), "갱신 배치가 철회한 연동을 ACTIVE 로 되돌렸다");
    assertNull(oauthPort.findAccessToken(scopeKey), "갱신 배치가 철회한 연동에 새 토큰을 발급받았다");
  }

  @Test
  @DisplayName("토큰 갱신은 동의 만료일을 늘리지 않는다")
  void refreshDoesNotExtendConsentExpiry() {
    // 동의 유효기간은 정보주체가 동의한 시점에 정해진다. 토큰을 갱신할 때마다 1년씩
    // 늘리면 사용자가 동의하지 않은 기간까지 수집하게 된다.
    LocalDateTime consentBefore = link().getConsentExpiresAt();

    runRefreshBatch();

    assertEquals(consentBefore, link().getConsentExpiresAt(), "토큰 갱신이 동의 만료일을 다시 썼다");
  }

  @Test
  @DisplayName("연동 해제도 같은 무효화를 탄다")
  void unlinkInvalidatesEverywhere() {
    assertTrue(authUseCase.unlinkInstitution(userId, INSTITUTION));

    assertNull(oauthPort.findAccessToken(scopeKey), "해제 후 런타임 토큰이 남았다");
    assertEquals(LinkStatus.REVOKED, link().getStatus(), "해제 후 원본 연동이 살아 있다");
    assertEquals("UNLINKED", linkStatus(), "해제 후 링크 상태가 해제로 남지 않았다");
  }

  @Test
  @DisplayName("이미 새어 남은 토큰은 다음 갱신 배치에서 정리된다")
  void leakedRuntimeTokensSelfHealOnNextBatch() {
    // 수정 전 코드가 남긴 상태를 그대로 만든다: 원본만 REVOKED, 런타임 토큰과 링크 상태는 ACTIVE.
    // 운영에는 이미 이런 연동이 있을 수 있으므로, 배포 뒤 첫 배치에서 정리되는지 본다.
    linkedInstitutionPersistence.markRevoked(userId, INSTITUTION);
    assertNotNull(oauthPort.findAccessToken(scopeKey), "재현 전제: 런타임 토큰이 남아 있어야 한다");

    runRefreshBatch();

    assertNull(oauthPort.findAccessToken(scopeKey), "새어 남은 런타임 토큰이 정리되지 않았다");
    assertEquals(LinkStatus.REVOKED, link().getStatus(), "배치가 원본 연동을 되살렸다");
    assertEquals("REVOKED", linkStatus(), "링크 상태가 원본과 어긋난 채 남았다");
  }

  @Test
  @DisplayName("대조: ACTIVE 연동은 갱신 배치가 실제로 토큰을 바꾼다")
  void refreshBatchRefreshesActiveLink() {
    // 이 검사가 통과해야 위의 "되살리지 않는다" 가 배치가 아예 안 돌아서 통과한 것이
    // 아님을 보장한다.
    String before = oauthPort.findAccessToken(scopeKey);

    runRefreshBatch();

    assertNotEquals(before, oauthPort.findAccessToken(scopeKey), "갱신 배치가 돌지 않았다");
    assertEquals(LinkStatus.ACTIVE, link().getStatus());
  }

  /**
   * 갱신 배치를 한 번 돌린다.
   *
   * <p>ShedLock 은 직접 호출도 가로챈다. lockAtLeastFor(1분) 안의 두 번째 호출은 예외 없이 건너뛰어, 배치가 안 돈 채로 검사가 통과할 수 있다.
   *
   * <p>락 행을 지우면 안 된다. 락 제공자는 한 번 넣은 행을 기억해 두고 다음부터 UPDATE 로만 잡으려 해서, 행이 없으면 0건이 갱신되고 역시 조용히 건너뛴다.
   * 처음에 그렇게 짰다가 대조 검사가 잡았다. 행은 두고 만료 시각만 과거로 돌린다.
   */
  private void runRefreshBatch() {
    jdbcTemplate.update(
        "UPDATE shedlock SET lock_until = ? WHERE name = ?",
        Timestamp.valueOf("2000-01-01 00:00:00"),
        "MyDataTokenRefreshBatch");
    refreshBatch.refreshExpiringTokens();
  }

  private String linkStatus() {
    return linkStatusRepository
        .findByUserIdAndInstitutionCode(userId, INSTITUTION)
        .orElseThrow()
        .getStatus();
  }

  private LinkedInstitutionEntity link() {
    return linkedInstitutionRepository
        .findByUser_UserIdAndInstitutionCode(Long.parseLong(userId), INSTITUTION)
        .orElseThrow();
  }

  private String createUser() {
    String nonce = UUID.randomUUID().toString().replace("-", "");
    UserEntity user = new UserEntity();
    user.setCiHash(nonce + nonce);
    user.setCi("ci-" + nonce);
    user.setPhoneHash(nonce + "0".repeat(32));
    user.setPhone("01012345678");
    user.setStatus(UserEntity.UserStatus.ACTIVE);
    user.setFailedLoginCount(0);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    return userRepository.save(user).getUserId().toString();
  }
}
