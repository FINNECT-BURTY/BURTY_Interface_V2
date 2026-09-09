/**
 *
 *
 * <pre>
 * <b>Description  : 보안 (WebAuthnSignature)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.out.security
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
package com.burty.adapter.out.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class WebAuthnSignature {

  private WebAuthnSignature() {}

  /**
   * HMAC-SHA256 서명.
   *
   * <p>예전에는 예외를 삼키고 빈 문자열을 돌려줬다. 그런데 호출부가 그 값을 그대로 비교하므로, 서버 시크릿이 비어 있으면 기대값도 {@code ""} 가 되고 공격자가
   * 접두사만 있는 토큰을 보내면 {@code "".equals("")} 로 인증이 통과했다. 시크릿을 지우는 것이 인증을 끄는 결과가 됐다.
   *
   * <p>HmacSHA256 은 JVM 표준이라 실제 실패 원인은 잘못된 키뿐이고, 그건 설정 오류이므로 드러나야 한다.
   *
   * @throws IllegalStateException 시크릿이 비었거나 서명할 수 없을 때
   */
  static String sign(String raw, String serverSecret) {
    if (serverSecret == null || serverSecret.isEmpty()) {
      throw new IllegalStateException("burty.webauthn.server-secret 이 비어 있습니다. 서명할 수 없습니다.");
    }
    try {
      Mac hmac = Mac.getInstance("HmacSHA256");
      hmac.init(new SecretKeySpec(serverSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(hmac.doFinal(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("WebAuthn 서명에 실패했습니다.", e);
    }
  }

  /**
   * 서명 비교.
   *
   * <p>{@code String.equals} 는 다른 바이트를 만나면 즉시 끝나므로 응답 시간으로 서명을 한 바이트씩 맞춰갈 수 있다. 길이가 같은 동안 상수 시간으로
   * 비교한다.
   */
  static boolean matches(String expected, String actual) {
    if (expected == null || actual == null || expected.isEmpty() || actual.isEmpty()) {
      return false;
    }
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
  }
}
