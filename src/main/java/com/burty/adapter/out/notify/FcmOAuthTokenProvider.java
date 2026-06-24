package com.burty.adapter.out.notify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** Firebase Cloud Messaging HTTP v1용 Google OAuth2 access token 발급. */
@Component
public class FcmOAuthTokenProvider {

  private static final String FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
  private static final String DEFAULT_TOKEN_URI = "https://oauth2.googleapis.com/token";

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final ConcurrentHashMap<String, CachedToken> cache = new ConcurrentHashMap<>();

  public FcmOAuthTokenProvider(RestTemplate restTemplate, ObjectMapper objectMapper) {
    this.restTemplate = restTemplate;
    this.objectMapper = objectMapper;
  }

  public String getAccessToken(String credentialsJson) {
    CachedToken cached = cache.get(credentialsJson);
    if (cached != null && cached.expiresAt().isAfter(Instant.now().plusSeconds(60))) {
      return cached.token();
    }
    try {
      ServiceAccountCredentials credentials =
          objectMapper.readValue(credentialsJson, ServiceAccountCredentials.class);
      String jwt = buildJwt(credentials);
      String tokenUri =
          credentials.tokenUri() != null && !credentials.tokenUri().isBlank()
              ? credentials.tokenUri()
              : DEFAULT_TOKEN_URI;
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
      String body =
          "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=" + jwt;
      String responseBody =
          restTemplate.postForObject(tokenUri, new HttpEntity<>(body, headers), String.class);
      if (responseBody == null || responseBody.isBlank()) {
        throw new IllegalStateException("FCM OAuth token response was empty");
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
      if (response == null || response.get("access_token") == null) {
        throw new IllegalStateException("FCM OAuth token response missing access_token");
      }
      String accessToken = String.valueOf(response.get("access_token"));
      long expiresIn =
          response.get("expires_in") instanceof Number number ? number.longValue() : 3600L;
      cache.put(
          credentialsJson,
          new CachedToken(accessToken, Instant.now().plusSeconds(Math.max(60, expiresIn - 30))));
      return accessToken;
    } catch (Exception e) {
      throw new IllegalStateException("FCM OAuth token issuance failed", e);
    }
  }

  private String buildJwt(ServiceAccountCredentials credentials) throws Exception {
    Instant now = Instant.now();
    PrivateKey privateKey = parsePrivateKey(credentials.privateKey());
    return Jwts.builder()
        .header()
        .add("typ", "JWT")
        .and()
        .issuer(credentials.clientEmail())
        .subject(credentials.clientEmail())
        .audience()
        .add(
            credentials.tokenUri() != null && !credentials.tokenUri().isBlank()
                ? credentials.tokenUri()
                : DEFAULT_TOKEN_URI)
        .and()
        .claim("scope", FCM_SCOPE)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(3600)))
        .signWith(privateKey, Jwts.SIG.RS256)
        .compact();
  }

  private static PrivateKey parsePrivateKey(String pem) throws Exception {
    String normalized =
        pem.replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\n", "\n")
            .replaceAll("\\s", "");
    byte[] decoded = Base64.getDecoder().decode(normalized);
    return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record ServiceAccountCredentials(
      String type, String project_id, String private_key, String client_email, String token_uri) {

    String privateKey() {
      return private_key;
    }

    String clientEmail() {
      return client_email;
    }

    String tokenUri() {
      return token_uri;
    }
  }

  private record CachedToken(String token, Instant expiresAt) {}
}
