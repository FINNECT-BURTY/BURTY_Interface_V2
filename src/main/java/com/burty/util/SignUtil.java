/**
 *
 *
 * <pre>
 * <b>Description  : 유틸 (SignUtil)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.util
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
package com.burty.util;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** HMAC 기반 단기 서명 URL/토큰 검증. */
@Component
public class SignUtil {

  private final String secret;
  private final long defaultTtlSeconds;

  public SignUtil(
      @Value("${burty.sign.secret:change-me-burty-sign-secret}") String secret,
      @Value("${burty.sign.ttl-seconds:10800}") long defaultTtlSeconds) {
    this.secret = secret;
    this.defaultTtlSeconds = defaultTtlSeconds;
  }

  public String signShort(String subject, long epochSeconds) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] sig = mac.doFinal((subject + "|" + epochSeconds).getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
    } catch (Exception e) {
      throw new IllegalStateException("HMAC sign failed", e);
    }
  }

  public boolean verifyShort(String subject, long epochSeconds, String signature) {
    return verifyShort(subject, epochSeconds, signature, defaultTtlSeconds);
  }

  public boolean verifyShort(String subject, long epochSeconds, String signature, long ttlSeconds) {
    if (signature == null || !signature.equals(signShort(subject, epochSeconds))) {
      return false;
    }
    long now = Instant.now().getEpochSecond();
    return now <= epochSeconds + ttlSeconds;
  }
}
