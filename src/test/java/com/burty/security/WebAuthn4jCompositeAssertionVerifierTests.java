package com.burty.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.AttestationConveyancePreference;
import com.webauthn4j.data.AuthenticatorAssertionResponse;
import com.webauthn4j.data.AuthenticatorAttachment;
import com.webauthn4j.data.AuthenticatorAttestationResponse;
import com.webauthn4j.data.AuthenticatorSelectionCriteria;
import com.webauthn4j.data.PublicKeyCredential;
import com.webauthn4j.data.PublicKeyCredentialCreationOptions;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialRequestOptions;
import com.webauthn4j.data.PublicKeyCredentialRpEntity;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.PublicKeyCredentialUserEntity;
import com.webauthn4j.data.ResidentKeyRequirement;
import com.webauthn4j.data.UserVerificationRequirement;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.data.extension.client.AuthenticationExtensionClientOutput;
import com.webauthn4j.data.extension.client.RegistrationExtensionClientOutput;
import com.webauthn4j.test.EmulatorUtil;
import com.webauthn4j.test.client.ClientPlatform;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * WebAuthn(패스키) 검증 경로.
 *
 * <p>서명 검증 코드는 실제 페이로드를 만들어보지 않으면 검증할 수 없다. 가짜 입력으로는 "폴백으로 빠진다" 만 확인되고, 정작 중요한 <b>진짜 서명을 통과시키는가</b>
 * 와 <b>재생 공격을 막는가</b>는 하나도 확인되지 않는다.
 *
 * <p>그래서 webauthn4j 의 인증기 에뮬레이터로 등록·인증 페이로드를 실제로 만들어 검증한다.
 *
 * <p>이 테스트가 있어야 라이브러리 API 이전(예: {@code AuthenticatorImpl} → {@code CredentialRecordImpl})을 안전하게 할 수
 * 있다.
 */
class WebAuthn4jCompositeAssertionVerifierTests {

  private static final String ORIGIN = "https://burty.example";
  private static final String RP_ID = "burty.example";

  private final ObjectConverter objectConverter = new ObjectConverter();

  private WebAuthn4jCompositeAssertionVerifier verifier;
  private ClientPlatform client;

  @BeforeEach
  void setUp() {
    verifier = new WebAuthn4jCompositeAssertionVerifier();
    client = EmulatorUtil.createClientPlatform(EmulatorUtil.NONE_ATTESTATION_AUTHENTICATOR);
    client.setOrigin(new Origin(ORIGIN));
  }

  // ── 정상 경로 ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("에뮬레이터가 만든 등록 응답을 검증하고 자격증명을 꺼낸다")
  void verifiesRealRegistration() {
    Registered registered = register("register-challenge-0001");

    assertTrue(registered.result().isVerified(), "정상 등록이 실패로 판정됐다");
    assertFalse(registered.result().getCredentialIdRaw().isEmpty(), "자격증명 ID 가 비었다");
    assertFalse(registered.result().getPublicKey().isEmpty(), "COSE 공개키가 비었다");
  }

  @Test
  @DisplayName("등록한 자격증명으로 서명한 인증 응답을 통과시킨다")
  void verifiesRealAuthentication() {
    Registered registered = register("register-challenge-0002");
    String challenge = "auth-challenge-0002";

    WebAuthnAssertionVerifier.VerificationResult result =
        verifier.verifyAuthentication(
            assertionPayload(challenge), challenge, ORIGIN, RP_ID, 0L, registered.stored(0L));

    assertTrue(result.isVerified(), "정상 인증이 실패로 판정됐다");
    // 사인 카운트는 인증기가 증가시킨다. 그대로 두면 재생 공격을 막을 수 없다.
    assertTrue(result.getNextSignCount() > 0, "사인 카운트가 증가하지 않았다: " + result.getNextSignCount());
  }

  // ── 실패 경로 ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("사인 카운트가 늘지 않으면 재생으로 보고 거부한다")
  void rejectsReplayedAssertion() {
    Registered registered = register("register-challenge-0003");
    String challenge = "auth-challenge-0003";
    String payload = assertionPayload(challenge);

    // 저장된 사인 카운트를 인증기가 보낸 값 이상으로 올려둔다 = 이미 쓴 어설션을 다시 낸 상황.
    WebAuthnAssertionVerifier.VerificationResult first =
        verifier.verifyAuthentication(payload, challenge, ORIGIN, RP_ID, 0L, registered.stored(0L));
    assertTrue(first.isVerified());

    // 어댑터는 저장된 사인 카운트를 currentSignCount 와 storedCredential 양쪽에 같은 값으로 넘긴다.
    long stored = first.getNextSignCount();
    WebAuthnAssertionVerifier.VerificationResult replayed =
        verifier.verifyAuthentication(
            payload, challenge, ORIGIN, RP_ID, stored, registered.stored(stored));

    assertFalse(replayed.isVerified(), "재생된 어설션이 통과했다");
    assertEquals(first.getNextSignCount(), replayed.getNextSignCount(), "거부 시 사인 카운트를 올리면 안 된다");
  }

  @Test
  @DisplayName("다른 챌린지로 서명한 어설션은 통과하지 못한다")
  void rejectsWrongChallenge() {
    Registered registered = register("register-challenge-0004");
    String payload = assertionPayload("auth-challenge-signed");

    WebAuthnAssertionVerifier.VerificationResult result =
        verifier.verifyAuthentication(
            payload, "auth-challenge-expected", ORIGIN, RP_ID, 0L, registered.stored(0L));

    assertFalse(result.isVerified(), "챌린지가 다른 어설션이 통과했다");
  }

  @Test
  @DisplayName("다른 오리진을 기대하면 통과하지 못한다")
  void rejectsWrongOrigin() {
    Registered registered = register("register-challenge-0005");
    String challenge = "auth-challenge-0005";

    WebAuthnAssertionVerifier.VerificationResult result =
        verifier.verifyAuthentication(
            assertionPayload(challenge),
            challenge,
            "https://attacker.example",
            RP_ID,
            0L,
            registered.stored(0L));

    assertFalse(result.isVerified(), "오리진이 다른 어설션이 통과했다");
  }

  @Test
  @DisplayName("서명 없이 기대값 문자열만 담은 위조 페이로드는 거부한다")
  void rejectsForgedPayloadWithoutSignature() {
    // 예전에는 이 페이로드가 통과했다. webauthn4j 가 파싱에 실패하면 문자열 일치만 보는
    // 검증기로 폴백했고, 그 검증기는 challenge·origin·rpId 문자열과 "signature" 키만 확인했다.
    // 챌린지는 서버가 클라이언트에 내려주는 값이고 오리진과 rpId 는 공개 정보다.
    // 즉 정상 로그인한 사용자가 이체의 생체인증 게이트를 그냥 넘길 수 있었다.
    String challenge = "server-issued-challenge";
    String forged =
        "{\"challenge\":\""
            + challenge
            + "\",\"origin\":\""
            + ORIGIN
            + "\",\"rpId\":\""
            + RP_ID
            + "\",\"signature\":\"AAAA\"}";
    WebAuthnStoredCredential stored =
        new WebAuthnStoredCredential(new byte[] {1, 2, 3}, new byte[] {4, 5, 6}, 0L);

    assertFalse(
        verifier.verifyAuthentication(forged, challenge, ORIGIN, RP_ID, 0L, stored).isVerified(),
        "서명 없는 위조 페이로드가 통과했다");
    assertFalse(
        verifier.verifyRegistration(forged, challenge, ORIGIN, RP_ID, 0L).isVerified(),
        "서명 없는 위조 등록 페이로드가 통과했다");
  }

  @Test
  @DisplayName("해석할 수 없는 페이로드는 실패로 처리한다 (사인 카운트는 올리지 않는다)")
  void rejectsUnparseablePayload() {
    WebAuthnAssertionVerifier.VerificationResult result =
        verifier.verifyRegistration("not-a-webauthn-payload", "c", ORIGIN, RP_ID, 7L);

    assertFalse(result.isVerified());
    // 실패할 때 사인 카운트를 올리면 뒤이은 정상 인증이 재생으로 오인돼 거부된다.
    assertEquals(7L, result.getNextSignCount());
  }

  // ── 도우미 ────────────────────────────────────────────────────────────────

  private record Registered(WebAuthnAssertionVerifier.VerificationResult result) {
    WebAuthnStoredCredential stored(long signCount) {
      Base64.Decoder decoder = Base64.getUrlDecoder();
      return new WebAuthnStoredCredential(
          decoder.decode(result.getCredentialIdRaw()),
          decoder.decode(result.getPublicKey()),
          signCount);
    }
  }

  private Registered register(String challengeText) {
    Challenge challenge = challengeOf(challengeText);
    PublicKeyCredentialCreationOptions options =
        new PublicKeyCredentialCreationOptions(
            new PublicKeyCredentialRpEntity(RP_ID, "BURTY"),
            new PublicKeyCredentialUserEntity(new byte[32], "burty-user", "burty-user"),
            challenge,
            List.of(
                new PublicKeyCredentialParameters(
                    PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256)),
            null,
            null,
            // 에뮬레이터는 authenticatorSelection 이 없으면 NPE 를 낸다.
            new AuthenticatorSelectionCriteria(
                AuthenticatorAttachment.PLATFORM,
                ResidentKeyRequirement.PREFERRED,
                UserVerificationRequirement.PREFERRED),
            AttestationConveyancePreference.NONE,
            null);

    PublicKeyCredential<AuthenticatorAttestationResponse, RegistrationExtensionClientOutput>
        credential = client.create(options);
    String payload = objectConverter.getJsonMapper().writeValueAsString(credential);

    WebAuthnAssertionVerifier.VerificationResult result =
        verifier.verifyRegistration(payload, challengeText, ORIGIN, RP_ID, 0L);
    return new Registered(result);
  }

  private String assertionPayload(String challengeText) {
    PublicKeyCredentialRequestOptions options =
        new PublicKeyCredentialRequestOptions(
            challengeOf(challengeText),
            null,
            RP_ID,
            null,
            UserVerificationRequirement.PREFERRED,
            null);
    PublicKeyCredential<AuthenticatorAssertionResponse, AuthenticationExtensionClientOutput>
        assertion = client.get(options);
    return objectConverter.getJsonMapper().writeValueAsString(assertion);
  }

  /** 검증기는 기대 챌린지 문자열의 UTF-8 바이트를 그대로 챌린지로 쓴다. 테스트도 같은 규칙을 따라야 한다. */
  private static Challenge challengeOf(String text) {
    return new DefaultChallenge(text.getBytes(StandardCharsets.UTF_8));
  }
}
