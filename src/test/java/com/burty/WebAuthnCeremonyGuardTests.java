package com.burty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.burty.adapter.out.security.WebAuthnCredentialStore;
import com.burty.adapter.out.security.WebAuthnDeviceTrustManager;
import com.burty.adapter.out.security.WebAuthnFido2Adapter;
import com.burty.adapter.out.store.ChallengeStore;
import com.burty.application.service.support.AuditLogger;
import com.burty.config.WebAuthnProperties;
import com.burty.domain.admin.model.AuditEvent;
import com.burty.security.JwtTokenProvider;
import com.burty.security.WebAuthnAssertionVerifier;
import com.burty.security.WebAuthnAssertionVerifier.VerificationResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * WebAuthn 의식(ceremony) 경계를 못 박는다.
 *
 * <p>여기서 지키려는 것은 두 가지다. <b>챌린지는 검증 성공 여부와 무관하게 한 번만 쓸 수 있어야 하고</b>, 등록용으로 발급한 챌린지를 인증에 쓸 수 없어야 한다.
 * 둘 중 하나라도 새면 서명을 다시 던져보며 맞춰볼 여지가 생긴다.
 *
 * <p>실제 서명 검증은 WebAuthn4j 가 한다. 여기서는 그 앞뒤의 판단만 본다 — 검증기를 모의로 두어 "검증이 통과했을 때/실패했을 때" 어댑터가 무엇을 하는지
 * 확인한다.
 */
class WebAuthnCeremonyGuardTests {

  private static final String USER_ID = "1";
  private static final String CHALLENGE_ID = "challenge-1";
  private static final String PAYLOAD = "signed-payload";

  private ChallengeStore challengeStore;
  private WebAuthnAssertionVerifier verifier;
  private WebAuthnCredentialStore credentialStore;
  private WebAuthnDeviceTrustManager deviceTrust;
  private WebAuthnFido2Adapter adapter;

  @BeforeEach
  void setUp() {
    challengeStore = mock(ChallengeStore.class);
    verifier = mock(WebAuthnAssertionVerifier.class);
    credentialStore = mock(WebAuthnCredentialStore.class);
    deviceTrust = mock(WebAuthnDeviceTrustManager.class);

    List<AuditEvent> audited = new ArrayList<>();
    AuditLogger auditLogger =
        new AuditLogger(audited::add, new SingletonProvider<>(new SimpleMeterRegistry()));

    adapter =
        new WebAuthnFido2Adapter(
            new WebAuthnProperties(),
            challengeStore,
            verifier,
            credentialStore,
            deviceTrust,
            mock(JwtTokenProvider.class),
            auditLogger);
  }

  /** 챌린지가 살아 있고 이 호출이 선점에 성공한 상태. */
  private void challengeIssuedFor(String userId, String flowType) {
    when(challengeStore.get(CHALLENGE_ID)).thenReturn(userId + "|" + flowType);
    when(challengeStore.consume(CHALLENGE_ID)).thenReturn(true);
  }

  private void verificationSucceeds() {
    when(verifier.verifyAuthentication(
            anyString(), anyString(), anyString(), anyString(), anyLong(), any()))
        .thenReturn(new VerificationResult(true, 1L, "cred", "key"));
    when(verifier.verifyRegistration(anyString(), anyString(), anyString(), anyString(), anyLong()))
        .thenReturn(new VerificationResult(true, 1L, "cred", "key"));
  }

  @Test
  @DisplayName("없는 챌린지는 검증까지 가지 않는다")
  void unknownChallengeIsRejected() {
    when(challengeStore.get(CHALLENGE_ID)).thenReturn(null);

    assertFalse(
        adapter.verifyAndConsumeChallenge(USER_ID, CHALLENGE_ID, PAYLOAD, "AUTHENTICATION"));
    verify(verifier, never())
        .verifyAuthentication(anyString(), anyString(), anyString(), anyString(), anyLong(), any());
  }

  @Test
  @DisplayName("이미 소비된 챌린지는 거부한다")
  void alreadyConsumedChallengeIsRejected() {
    // 선점에 실패했다는 것은 다른 요청이 먼저 썼다는 뜻이다. 값이 아직 보여도 안 된다.
    when(challengeStore.get(CHALLENGE_ID)).thenReturn(USER_ID + "|AUTHENTICATION");
    when(challengeStore.consume(CHALLENGE_ID)).thenReturn(false);

    assertFalse(
        adapter.verifyAndConsumeChallenge(USER_ID, CHALLENGE_ID, PAYLOAD, "AUTHENTICATION"));
  }

  @Test
  @DisplayName("검증에 실패해도 챌린지는 소비한다")
  void challengeIsConsumedEvenWhenVerificationFails() {
    // 성공했을 때만 지우면 실패한 시도가 챌린지를 남긴다. TTL 이 다할 때까지 같은
    // 챌린지로 서명을 몇 번이든 다시 던져볼 수 있게 된다.
    challengeIssuedFor(USER_ID, "AUTHENTICATION");
    when(verifier.verifyAuthentication(
            anyString(), anyString(), anyString(), anyString(), anyLong(), any()))
        .thenReturn(new VerificationResult(false, 0L, null, null));

    assertFalse(
        adapter.verifyAndConsumeChallenge(USER_ID, CHALLENGE_ID, PAYLOAD, "AUTHENTICATION"));
    verify(challengeStore).consume(CHALLENGE_ID);
  }

  @Test
  @DisplayName("등록용 챌린지를 인증에 쓸 수 없다")
  void registrationChallengeCannotBeUsedForAuthentication() {
    // 의식을 구분하지 않으면 등록 과정에서 받은 서명을 인증으로 재사용할 여지가 생긴다.
    challengeIssuedFor(USER_ID, "REGISTRATION");
    verificationSucceeds();

    assertFalse(
        adapter.verifyAndConsumeChallenge(USER_ID, CHALLENGE_ID, PAYLOAD, "AUTHENTICATION"));
  }

  @Test
  @DisplayName("남의 챌린지로는 통과할 수 없다")
  void challengeOfAnotherUserIsRejected() {
    challengeIssuedFor("999", "AUTHENTICATION");
    verificationSucceeds();

    assertFalse(
        adapter.verifyAndConsumeChallenge(USER_ID, CHALLENGE_ID, PAYLOAD, "AUTHENTICATION"));
  }

  @Test
  @DisplayName("검증이 실패하면 자격증명을 갱신하지 않는다")
  void failedVerificationDoesNotTouchCredentials() {
    // 서명 카운터가 올라가면 정상 기기의 다음 인증이 거부될 수 있다.
    challengeIssuedFor(USER_ID, "AUTHENTICATION");
    when(verifier.verifyAuthentication(
            anyString(), anyString(), anyString(), anyString(), anyLong(), any()))
        .thenReturn(new VerificationResult(false, 0L, null, null));

    adapter.verifyAndConsumeChallenge(USER_ID, CHALLENGE_ID, PAYLOAD, "AUTHENTICATION");

    verify(credentialStore, never()).updateAfterAuthentication(anyLong(), any());
    verify(credentialStore, never()).upsertFromRegistration(anyLong(), any(), any());
  }

  @Test
  @DisplayName("정상 인증은 통과하고 자격증명을 갱신한다")
  void validAuthenticationPasses() {
    // 위 거부들이 "전부 거부" 로 통과하는 가짜 테스트가 아님을 보인다.
    challengeIssuedFor(USER_ID, "AUTHENTICATION");
    verificationSucceeds();

    assertTrue(adapter.verifyAndConsumeChallenge(USER_ID, CHALLENGE_ID, PAYLOAD, "AUTHENTICATION"));
    verify(credentialStore).updateAfterAuthentication(eq(1L), any());
  }

  @Test
  @DisplayName("발급한 챌린지는 설정한 TTL 로 저장한다")
  void challengeIsStoredWithConfiguredTtl() {
    // TTL 이 없으면 챌린지가 영원히 남는다. 일회성만으로는 부족하다.
    WebAuthnProperties properties = new WebAuthnProperties();

    String issued = adapter.issueChallenge(USER_ID, "AUTHENTICATION");

    verify(challengeStore)
        .put(eq(issued), eq(USER_ID + "|AUTHENTICATION"), eq(properties.getChallengeTtlSeconds()));
  }

  private record SingletonProvider<T>(T instance) implements ObjectProvider<T> {
    @Override
    public T getObject() {
      return instance;
    }

    @Override
    public T getObject(Object... args) {
      return instance;
    }

    @Override
    public T getIfAvailable() {
      return instance;
    }

    @Override
    public T getIfUnique() {
      return instance;
    }
  }
}
