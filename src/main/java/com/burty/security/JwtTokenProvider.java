/**
 *
 *
 * <pre>
 * <b>Description  : 보안 (JwtTokenProvider)</b>
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
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
  private final SecretKey secretKey;
  private final JwtProperties jwtProperties;

  public JwtTokenProvider(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
    String keyRaw = (jwtProperties.getSecret() + "01234567890123456789012345678901");
    this.secretKey = Keys.hmacShaKeyFor(keyRaw.substring(0, 32).getBytes(StandardCharsets.UTF_8));
  }

  public String generateToken(String userId) {
    return generateToken(userId, "ROLE_USER");
  }

  public String generateToken(String userId, String role) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + (jwtProperties.getExpirationSeconds() * 1000));
    return Jwts.builder()
        .subject(userId)
        .claim("role", role)
        .issuedAt(now)
        .expiration(expiry)
        .signWith(secretKey)
        .compact();
  }

  public String getUserId(String token) {
    return parseClaims(token).getSubject();
  }

  public String getRole(String token) {
    Object role = parseClaims(token).get("role");
    return role != null ? role.toString() : "ROLE_USER";
  }

  public boolean validateToken(String token) {
    try {
      Claims claims = parseClaims(token);
      return claims.getExpiration() != null && claims.getExpiration().after(new Date());
    } catch (Exception e) {
      return false;
    }
  }

  /** Redis 블랙리스트 TTL을 JWT 만료 시각에 맞추기 위한 남은 유효 시간(초). */
  public long getRemainingTtlSeconds(String token) {
    try {
      Claims claims = parseClaims(token);
      Date exp = claims.getExpiration();
      if (exp == null) {
        return jwtProperties.getExpirationSeconds();
      }
      long seconds = (exp.getTime() - System.currentTimeMillis() + 999L) / 1000L;
      return Math.max(1L, Math.min(seconds, jwtProperties.getExpirationSeconds()));
    } catch (Exception e) {
      return jwtProperties.getExpirationSeconds();
    }
  }

  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
  }
}
