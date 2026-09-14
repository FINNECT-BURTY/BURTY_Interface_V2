package com.burty;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.burty.application.dto.auth.SignupConsents;
import com.burty.application.service.auth.UserOnboardingService;
import com.burty.application.service.support.AuditLogger;
import com.burty.config.BurtyOnboardingProperties;
import com.burty.domain.user.entity.ConsentRecordEntity;
import com.burty.domain.user.entity.ConsentRecordEntity.ConsentType;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.ConsentRecordRepository;
import com.burty.domain.user.repository.UserProfileRepository;
import com.burty.domain.user.repository.UserRepository;
import com.burty.util.AccountNumberHasher;
import java.net.InetAddress;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 가입 동의 기록.
 *
 * <p>화면은 여섯 항목에 동의를 받는데 백엔드에는 {@code termsAccepted=true} 하나만 왔고, 이용약관·개인정보 두 건만 기록됐다. 수집·이용, 전송요구,
 * 마케팅, 혜택 동의는 흔적이 없었다. 동의는 "받았다" 가 아니라 무엇에·언제·어떤 버전으로 받았는지 증명할 수 있어야 한다.
 */
class UserOnboardingConsentTests {

  private static final String USER_ID = "7";

  private ConsentRecordRepository consents;
  private UserOnboardingService service;

  @BeforeEach
  void setUp() {
    UserEntity user = new UserEntity();
    user.setUserId(7L);

    UserRepository users = mock(UserRepository.class);
    when(users.findById(anyLong())).thenReturn(Optional.of(user));
    when(users.findByPhoneHash(anyString())).thenReturn(Optional.empty());
    when(users.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    UserProfileRepository profiles = mock(UserProfileRepository.class);
    when(profiles.existsById(anyLong())).thenReturn(false);

    consents = mock(ConsentRecordRepository.class);

    service =
        new UserOnboardingService(
            users,
            profiles,
            consents,
            new BurtyOnboardingProperties(),
            new AccountNumberHasher(),
            mock(AuditLogger.class));
  }

  private void complete(SignupConsents agreed, String ip, String userAgent) {
    service.completeProfile(
        USER_ID, "01012345678", "홍길동", LocalDate.of(1962, 3, 4), 60, null, agreed, ip, userAgent);
  }

  private List<ConsentRecordEntity> saved() {
    ArgumentCaptor<ConsentRecordEntity> captor = ArgumentCaptor.forClass(ConsentRecordEntity.class);
    verify(consents, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
    return captor.getAllValues();
  }

  @Test
  @DisplayName("동의한 항목을 전부 기록한다")
  void recordsEveryAgreedConsent() {
    complete(
        new SignupConsents(true, true, true, true, true, true, true), "203.0.113.5", "Mozilla/5.0");

    Set<ConsentType> types =
        saved().stream().map(ConsentRecordEntity::getConsentType).collect(Collectors.toSet());
    assertEquals(
        Set.of(
            ConsentType.TERMS,
            ConsentType.PRIVACY,
            ConsentType.CREDIT_COLLECTION,
            ConsentType.MYDATA,
            ConsentType.MARKETING,
            ConsentType.BENEFIT_NOTICE,
            ConsentType.THIRD_PARTY_SHARE),
        types);
  }

  @Test
  @DisplayName("동의하지 않은 선택 항목은 남기지 않는다")
  void doesNotRecordRefusedOptionalConsents() {
    // 거부를 "동의함" 으로 적으면 기록이 거짓이 된다. 마케팅은 특히 분쟁이 생기는 항목이다.
    complete(
        new SignupConsents(true, true, true, true, false, false, false),
        "203.0.113.5",
        "Mozilla/5.0");

    Set<ConsentType> types =
        saved().stream().map(ConsentRecordEntity::getConsentType).collect(Collectors.toSet());
    assertFalse(types.contains(ConsentType.MARKETING));
    assertFalse(types.contains(ConsentType.BENEFIT_NOTICE));
    assertTrue(types.contains(ConsentType.CREDIT_COLLECTION));
  }

  @Test
  @DisplayName("동의 시점의 IP 와 User-Agent 를 함께 남긴다")
  void recordsEvidence() throws Exception {
    complete(
        new SignupConsents(true, true, true, true, false, false, false),
        "203.0.113.5",
        "BurtyApp/1.0");

    ConsentRecordEntity first = saved().get(0);
    assertArrayEquals(InetAddress.getByName("203.0.113.5").getAddress(), first.getIpAddress());
    assertEquals("BurtyApp/1.0", first.getUserAgent());
  }

  @Test
  @DisplayName("IP 를 확인할 수 없으면 비워 둔다 — 틀린 값을 남기지 않는다")
  void leavesIpEmptyWhenUnknown() {
    complete(new SignupConsents(true, true, true, true, false, false, false), "unknown", null);

    ConsentRecordEntity first = saved().get(0);
    assertEquals(null, first.getIpAddress());
    assertEquals(null, first.getUserAgent());
  }

  @Test
  @DisplayName("국외 이전에 동의하지 않으면 기록을 남기지 않는다")
  void doesNotRecordRefusedOverseasTransfer() {
    // 이 기록이 없으면 운영에서 AI 상담·음성이 막힌다. 거부를 "동의함" 으로 적으면 그 차단이 무의미해진다 (#171).
    complete(
        new SignupConsents(true, true, true, true, false, false, false),
        "203.0.113.5",
        "Mozilla/5.0");

    Set<ConsentType> types =
        saved().stream().map(ConsentRecordEntity::getConsentType).collect(Collectors.toSet());
    assertFalse(types.contains(ConsentType.THIRD_PARTY_SHARE));
  }

  @Test
  @DisplayName("항목별 값을 보내지 않는 옛 클라이언트는 예전처럼 필수 약관 두 건만 남긴다")
  void legacyClientRecordsTermsAndPrivacyOnly() {
    // FE 와 백엔드는 따로 배포된다. 그 동안 가입이 막히면 안 된다.
    complete(SignupConsents.legacy(true), "203.0.113.5", "Mozilla/5.0");

    Set<ConsentType> types =
        saved().stream().map(ConsentRecordEntity::getConsentType).collect(Collectors.toSet());
    assertEquals(Set.of(ConsentType.TERMS, ConsentType.PRIVACY), types);
  }
}
