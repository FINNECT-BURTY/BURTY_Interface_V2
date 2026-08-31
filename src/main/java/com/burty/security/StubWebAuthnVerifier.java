/**
 *
 *
 * <pre>
 * <b>Description  : 보안 (StubWebAuthnVerifier)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.security
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
package com.burty.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 개발·테스트 전용 WebAuthn 검증기 <b>스텁</b>.
 *
 * <p><b>서명을 검증하지 않는다.</b> 페이로드에 기대 챌린지·오리진이 들어 있는지만 본다. 즉 위조한 JSON 을 그대로 통과시킨다.
 *
 * <p>실제 인증기 없이 이체 흐름을 돌려보기 위한 것이다. {@code burty.webauthn.stub-mode=true} 일 때만 만들어지고, 그 설정으로는 prod
 * 프로파일이 기동하지 않는다({@code ProdStartupValidator}).
 *
 * <p>예전에는 이 클래스가 {@code WebAuthn4jCompositeAssertionVerifier} 의 폴백으로 <b>운영에서도</b> 동작했다. 그래서 이체의
 * 생체인증 게이트를 위조 페이로드로 통과할 수 있었다.
 *
 * <h2>받아들이는 페이로드</h2>
 *
 * <p>두 가지를 모두 받는다.
 *
 * <ol>
 *   <li><b>실제 브라우저 페이로드</b> — {@code PublicKeyCredential#toJSON()}. {@code response.clientDataJSON}
 *       을 디코드해 검사한다.
 *   <li><b>평문 페이로드</b> — {@code challenge}·{@code origin}·{@code rpId} 를 최상위에 둔 형태. 브라우저 없이 도는 자동화
 *       테스트용이다.
 * </ol>
 *
 * <p>예전에는 2번만 받았다. 그래서 dev·staging 에서 <b>진짜 패스키로는 등록·인증이 통과하지 못했고</b>, 개발 중에 검증되는 경로가 운영에서 도는 경로와
 * 갈라져 있었다. 운영에서만 깨지는 설정 오류(프론트엔드가 아닌 백엔드 URL 을 {@code origin} 으로 둔 것 같은)를 개발 중에 만날 방법이 없는 구조였다.
 */
public class StubWebAuthnVerifier implements WebAuthnAssertionVerifier {

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(StubWebAuthnVerifier.class);

  /** {@code toJSON()} 의 {@code response.clientDataJSON}. base64url 이라 문자 집합이 제한돼 있다. */
  private static final Pattern CLIENT_DATA_JSON =
      Pattern.compile("\"clientDataJSON\"\\s*:\\s*\"([A-Za-z0-9_=-]+)\"");

  public StubWebAuthnVerifier() {
    log.warn("WebAuthn 스텁 검증기가 활성화됐다. 서명을 검증하지 않는다 — 운영에서 쓰면 안 된다.");
  }

  @Override
  public VerificationResult verifyRegistration(
      String payload,
      String expectedChallenge,
      String expectedOrigin,
      String expectedRpId,
      long currentSignCount) {
    boolean ok =
        accepts(payload, expectedChallenge, expectedOrigin, expectedRpId, "webauthn.create")
            && payload.contains("\"attestationObject\"");
    return new VerificationResult(ok, currentSignCount + 1, "cred-raw", "pub-key");
  }

  @Override
  public VerificationResult verifyAuthentication(
      String payload,
      String expectedChallenge,
      String expectedOrigin,
      String expectedRpId,
      long currentSignCount,
      WebAuthnStoredCredential storedCredential) {
    boolean ok =
        accepts(payload, expectedChallenge, expectedOrigin, expectedRpId, "webauthn.get")
            && payload.contains("\"signature\"");
    return new VerificationResult(ok, currentSignCount + 1, "cred-raw", "pub-key");
  }

  private boolean accepts(
      String payload, String challenge, String origin, String rpId, String expectedClientDataType) {
    if (payload == null || payload.isBlank()) return false;

    String clientData = decodeClientDataJson(payload);
    if (clientData != null) {
      return acceptsClientData(clientData, challenge, origin, expectedClientDataType);
    }

    return acceptsPlainFields(payload, challenge, origin, rpId);
  }

  /**
   * 실제 브라우저 페이로드.
   *
   * <p>{@code clientDataJSON} 안의 {@code challenge} 는 챌린지 <b>바이트를 base64url 로 인코딩한 것</b>이다. 서버가 준
   * 문자열과 직접 비교하면 언제나 어긋나므로, 같은 방식으로 인코딩해 비교한다.
   *
   * <p>{@code rpId} 는 여기서 보지 않는다. 그 값은 {@code authenticatorData} 의 {@code rpIdHash} 에 SHA-256 으로만
   * 남아 있어서, 확인하려면 CBOR 을 풀어야 한다. 스텁이 할 일은 아니다 — 운영 검증기가 본다.
   */
  private boolean acceptsClientData(
      String clientData, String challenge, String origin, String expectedType) {
    String encodedChallenge =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(challenge.getBytes(StandardCharsets.UTF_8));

    boolean ok =
        contains(clientData, "type", expectedType)
            && contains(clientData, "challenge", encodedChallenge)
            && contains(clientData, "origin", origin);

    if (!ok) {
      // origin 불일치는 설정 오류일 때가 많고, 그대로 두면 화면에는 "인증 실패" 로만 보인다.
      log.warn("스텁이 브라우저 페이로드를 거부했다 — 기대 origin {} / clientDataJSON {}", origin, clientData);
    }
    return ok;
  }

  /** 브라우저 없이 도는 자동화 테스트용 형태. */
  private boolean acceptsPlainFields(String payload, String challenge, String origin, String rpId) {
    return contains(payload, "challenge", challenge)
        && contains(payload, "origin", origin)
        && contains(payload, "rpId", rpId);
  }

  private String decodeClientDataJson(String payload) {
    Matcher matcher = CLIENT_DATA_JSON.matcher(payload);
    if (!matcher.find()) return null;

    try {
      return new String(Base64.getUrlDecoder().decode(matcher.group(1)), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      // clientDataJSON 이 있는데 디코드되지 않는다면 평문 형태로 되돌아갈 이유가 없다.
      // 빈 문자열을 돌려 검사를 실패시킨다.
      log.warn("clientDataJSON 을 base64url 로 디코드하지 못했다.");
      return "";
    }
  }

  /** JSON 안의 {@code "key":"value"} 를 찾는다. 값 자체에 따옴표가 없는 필드에만 쓴다. */
  private boolean contains(String json, String key, String value) {
    if (value == null) return false;
    return json.contains("\"" + key + "\":\"" + value + "\"")
        || json.contains("\"" + key + "\": \"" + value + "\"");
  }
}
