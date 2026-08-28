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

/**
 * 개발·테스트 전용 WebAuthn 검증기 <b>스텁</b>.
 *
 * <p><b>서명을 검증하지 않는다.</b> 페이로드에 기대 챌린지·오리진·rpId 문자열과 {@code "signature"} 키가 있는지만 본다. 즉 위조한 JSON 을
 * 그대로 통과시킨다.
 *
 * <p>실제 인증기 없이 이체 흐름을 돌려보기 위한 것이다. {@code burty.webauthn.stub-mode=true} 일 때만 만들어지고, 그 설정으로는 prod
 * 프로파일이 기동하지 않는다({@code ProdStartupValidator}).
 *
 * <p>예전에는 이 클래스가 {@code WebAuthn4jCompositeAssertionVerifier} 의 폴백으로 <b>운영에서도</b> 동작했다. 그래서 이체의
 * 생체인증 게이트를 위조 페이로드로 통과할 수 있었다.
 */
public class StubWebAuthnVerifier implements WebAuthnAssertionVerifier {

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(StubWebAuthnVerifier.class);

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
        containsStandardFields(payload, expectedChallenge, expectedOrigin, expectedRpId)
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
        containsStandardFields(payload, expectedChallenge, expectedOrigin, expectedRpId)
            && payload.contains("\"signature\"");
    return new VerificationResult(ok, currentSignCount + 1, "cred-raw", "pub-key");
  }

  private boolean containsStandardFields(
      String payload, String challenge, String origin, String rpId) {
    if (payload == null || payload.isBlank()) return false;
    return payload.contains("\"challenge\":\"" + challenge + "\"")
        && payload.contains("\"origin\":\"" + origin + "\"")
        && payload.contains("\"rpId\":\"" + rpId + "\"");
  }
}
