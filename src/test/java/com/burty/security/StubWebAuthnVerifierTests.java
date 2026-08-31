package com.burty.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 스텁이 받아들이는 페이로드의 모양.
 *
 * <p>스텁은 서명을 검증하지 않는다. 그래서 여기서 고정하는 것은 "안전한가" 가 아니라 <b>운영에서 도는 것과 같은 페이로드를 받는가</b> 다.
 *
 * <p>예전에는 평문 형태만 받았다. 그래서 dev·staging 에서 진짜 패스키로는 등록이 통과하지 못했고, 개발 중에 검증되는 경로가 운영 경로와 갈라져 있었다. 그
 * 상태에서는 origin 설정이 틀려도 개발 중에 드러나지 않는다.
 */
class StubWebAuthnVerifierTests {

  private static final String CHALLENGE = "challenge-id-1234";
  private static final String ORIGIN = "http://localhost:3000";
  private static final String RP_ID = "localhost";

  private final StubWebAuthnVerifier verifier = new StubWebAuthnVerifier();

  private static String encode(String value) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  /** 브라우저가 만드는 `PublicKeyCredential#toJSON()` 의 모양. */
  private static String browserPayload(String type, String origin, String challenge, String tail) {
    String clientData =
        "{\"type\":\"%s\",\"challenge\":\"%s\",\"origin\":\"%s\",\"crossOrigin\":false}"
            .formatted(type, encode(challenge), origin);
    return "{\"id\":\"cred\",\"rawId\":\"cred\",\"type\":\"public-key\",\"response\":{"
        + "\"clientDataJSON\":\""
        + encode(clientData)
        + "\","
        + tail
        + "}}";
  }

  private static String registrationPayload(String origin, String challenge) {
    return browserPayload("webauthn.create", origin, challenge, "\"attestationObject\":\"o=\"");
  }

  private static String authenticationPayload(String origin, String challenge) {
    return browserPayload(
        "webauthn.get", origin, challenge, "\"authenticatorData\":\"a=\",\"signature\":\"s=\"");
  }

  @Nested
  @DisplayName("실제 브라우저 페이로드")
  class BrowserPayload {

    @Test
    @DisplayName("등록 페이로드를 받는다")
    void acceptsRegistration() {
      var result =
          verifier.verifyRegistration(
              registrationPayload(ORIGIN, CHALLENGE), CHALLENGE, ORIGIN, RP_ID, 0);
      assertTrue(result.isVerified());
    }

    @Test
    @DisplayName("인증 페이로드를 받는다")
    void acceptsAuthentication() {
      var result =
          verifier.verifyAuthentication(
              authenticationPayload(ORIGIN, CHALLENGE), CHALLENGE, ORIGIN, RP_ID, 0, null);
      assertTrue(result.isVerified());
    }

    /**
     * origin 이 어긋나면 거부해야 한다.
     *
     * <p>이것이 스텁을 고친 이유다. 설정이 프론트엔드가 아닌 백엔드 URL 을 가리켜도 예전 스텁은 그 사실을 드러내지 못했고, 그대로 운영에 올라가 이체가 막히는
     * 형태로만 나타났다.
     */
    @Test
    @DisplayName("origin 이 다르면 거부한다")
    void rejectsWrongOrigin() {
      var result =
          verifier.verifyRegistration(
              registrationPayload("http://localhost:8080", CHALLENGE), CHALLENGE, ORIGIN, RP_ID, 0);
      assertFalse(result.isVerified());
    }

    @Test
    @DisplayName("challenge 가 다르면 거부한다")
    void rejectsWrongChallenge() {
      var result =
          verifier.verifyRegistration(
              registrationPayload(ORIGIN, "다른-챌린지"), CHALLENGE, ORIGIN, RP_ID, 0);
      assertFalse(result.isVerified());
    }

    /** 등록 페이로드로 인증을 통과하면 안 된다. `type` 이 의식을 구분한다. */
    @Test
    @DisplayName("등록 페이로드로 인증을 통과할 수 없다")
    void rejectsCeremonyMismatch() {
      var result =
          verifier.verifyAuthentication(
              registrationPayload(ORIGIN, CHALLENGE), CHALLENGE, ORIGIN, RP_ID, 0, null);
      assertFalse(result.isVerified());
    }

    @Test
    @DisplayName("clientDataJSON 이 디코드되지 않으면 평문 검사로 되돌아가지 않는다")
    void rejectsUndecodableClientData() {
      String payload =
          "{\"response\":{\"clientDataJSON\":\"====\"},"
              + "\"challenge\":\""
              + CHALLENGE
              + "\",\"origin\":\""
              + ORIGIN
              + "\",\"rpId\":\""
              + RP_ID
              + "\",\"attestationObject\":\"o\"}";
      var result = verifier.verifyRegistration(payload, CHALLENGE, ORIGIN, RP_ID, 0);
      assertFalse(result.isVerified());
    }
  }

  @Nested
  @DisplayName("평문 페이로드")
  class PlainPayload {

    private static String plain(String origin) {
      return "{\"challenge\":\"%s\",\"origin\":\"%s\",\"rpId\":\"%s\",\"attestationObject\":\"o\",\"signature\":\"\"}"
          .formatted(CHALLENGE, origin, RP_ID);
    }

    /** 브라우저 없이 도는 자동화 테스트가 이 형태를 쓴다. 계속 받아야 한다. */
    @Test
    @DisplayName("여전히 받는다")
    void stillAccepted() {
      var result = verifier.verifyRegistration(plain(ORIGIN), CHALLENGE, ORIGIN, RP_ID, 0);
      assertTrue(result.isVerified());
    }

    @Test
    @DisplayName("origin 이 다르면 거부한다")
    void rejectsWrongOrigin() {
      var result =
          verifier.verifyRegistration(plain("http://localhost:8080"), CHALLENGE, ORIGIN, RP_ID, 0);
      assertFalse(result.isVerified());
    }
  }

  @Nested
  @DisplayName("빈 페이로드")
  class EmptyPayload {

    @Test
    @DisplayName("null 과 공백을 거부한다")
    void rejected() {
      assertFalse(verifier.verifyRegistration(null, CHALLENGE, ORIGIN, RP_ID, 0).isVerified());
      assertFalse(verifier.verifyRegistration("  ", CHALLENGE, ORIGIN, RP_ID, 0).isVerified());
    }
  }
}
