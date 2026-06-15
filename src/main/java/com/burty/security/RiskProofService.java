/**
 *
 *
 * <pre>
 * <b>Description  : 보안 애플리케이션 서비스 (RiskProofService)</b>
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

import com.burty.config.JwtProperties;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class RiskProofService {
  private static final long LEVEL_2_TTL_SECONDS = 300L;
  private static final long LEVEL_3_TTL_SECONDS = 120L;
  private final byte[] signingKey;

  public RiskProofService(JwtProperties jwtProperties) {
    this.signingKey = ("risk-proof:" + jwtProperties.getSecret()).getBytes(StandardCharsets.UTF_8);
  }

  public String issue(String userId, RiskLevel level) {
    long expiresAt = Instant.now().getEpochSecond() + ttlByLevel(level);
    String nonce = UUID.randomUUID().toString();
    String payload = "%s|%s|%d|%s".formatted(userId, level.name(), expiresAt, nonce);
    String signature = sign(payload);
    String tokenRaw = payload + "|" + signature;
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(tokenRaw.getBytes(StandardCharsets.UTF_8));
  }

  public boolean verify(String token, String expectedUserId, RiskLevel requiredLevel) {
    if (token == null || token.isBlank()) {
      return false;
    }
    try {
      String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
      String[] parts = decoded.split("\\|");
      if (parts.length != 5) {
        return false;
      }
      String userId = parts[0];
      RiskLevel issuedLevel = RiskLevel.valueOf(parts[1]);
      long expiresAt = Long.parseLong(parts[2]);
      String nonce = parts[3];
      String signature = parts[4];

      if (!expectedUserId.equals(userId)) {
        return false;
      }
      if (!isAllowedFor(issuedLevel, requiredLevel)) {
        return false;
      }
      if (expiresAt < Instant.now().getEpochSecond()) {
        return false;
      }
      String expectedSig =
          sign("%s|%s|%d|%s".formatted(userId, issuedLevel.name(), expiresAt, nonce));
      return expectedSig.equals(signature);
    } catch (Exception ignored) {
      return false;
    }
  }

  private boolean isAllowedFor(RiskLevel issued, RiskLevel required) {
    if (required == RiskLevel.LEVEL_2) {
      return issued == RiskLevel.LEVEL_2 || issued == RiskLevel.LEVEL_3;
    }
    return issued == required;
  }

  private long ttlByLevel(RiskLevel level) {
    return level == RiskLevel.LEVEL_2 ? LEVEL_2_TTL_SECONDS : LEVEL_3_TTL_SECONDS;
  }

  private String sign(String raw) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
      byte[] digest = mac.doFinal(raw.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (Exception e) {
      return "";
    }
  }
}
