package com.burty.adapter.out.notify;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Solapi REST API HMAC-SHA256 Authorization 헤더 생성. */
public final class SolapiAuthHeaderBuilder {

  private SolapiAuthHeaderBuilder() {}

  public static String build(String apiKey, String apiSecret) {
    String date = Instant.now().toString();
    String salt = UUID.randomUUID().toString().replace("-", "");
    String signature = sign(apiSecret, date + salt);
    return "HMAC-SHA256 ApiKey="
        + apiKey
        + ", Date="
        + date
        + ", Salt="
        + salt
        + ", Signature="
        + signature;
  }

  static String sign(String apiSecret, String message) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("Solapi HMAC signing failed", e);
    }
  }
}
