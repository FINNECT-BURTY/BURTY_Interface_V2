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
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class WebAuthnSignature {

  private WebAuthnSignature() {}

  static String sign(String raw, String serverSecret) {
    try {
      Mac hmac = Mac.getInstance("HmacSHA256");
      hmac.init(new SecretKeySpec(serverSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(hmac.doFinal(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      return "";
    }
  }
}
