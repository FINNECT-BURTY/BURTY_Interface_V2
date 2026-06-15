package com.burty.adapter.out.mydata;

import com.burty.adapter.out.mydata.dto.MyDataTokenResponse;
import com.burty.adapter.out.store.TokenStore;
import com.burty.application.port.out.mydata.MyDataOAuthPort;
import com.burty.config.MyDataProperties;
import com.burty.domain.mydata.model.MyDataTokenBundle;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class MyDataOAuthAdapter implements MyDataOAuthPort {
  private static final String REFRESH_PREFIX = "refresh:";
  private static final String EXPIRES_PREFIX = "expires:";

  private final RestTemplate restTemplate;
  private final MyDataProperties properties;
  private final TokenStore tokenStore;

  public MyDataOAuthAdapter(
      RestTemplate restTemplate, MyDataProperties properties, TokenStore tokenStore) {
    this.restTemplate = restTemplate;
    this.properties = properties;
    this.tokenStore = tokenStore;
  }

  @Override
  public String buildAuthorizeUrl(String oauthState) {
    return UriComponentsBuilder.fromUriString(properties.getAuthorizeUrl())
        .queryParam("response_type", "code")
        .queryParam("client_id", properties.getClientId())
        .queryParam("redirect_uri", properties.getRedirectUri())
        .queryParam("scope", properties.getScope())
        .queryParam("state", oauthState)
        .build()
        .toUriString();
  }

  @Override
  public MyDataTokenBundle exchangeTokens(String scopeKey, String code) {
    if (code == null || code.isBlank()) {
      return null;
    }
    MyDataTokenResponse response = fetchTokenResponse(code);
    if (response == null
        || response.getAccessToken() == null
        || response.getAccessToken().isBlank()) {
      return null;
    }
    LocalDateTime expiresAt = toExpiresAt(response.getExpiresIn());
    storeTokens(scopeKey, response.getAccessToken(), response.getRefreshToken(), expiresAt);
    return new MyDataTokenBundle(response.getAccessToken(), response.getRefreshToken(), expiresAt);
  }

  @Override
  public String findAccessToken(String scopeKey) {
    return tokenStore.get(scopeKey);
  }

  @Override
  public String findRefreshToken(String scopeKey) {
    return tokenStore.get(REFRESH_PREFIX + scopeKey);
  }

  @Override
  public LocalDateTime findTokenExpiresAt(String scopeKey) {
    String raw = tokenStore.get(EXPIRES_PREFIX + scopeKey);
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      long epochMillis = Long.parseLong(raw);
      return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  @Override
  public String refreshAccessToken(String scopeKey) {
    String refreshToken = tokenStore.get(REFRESH_PREFIX + scopeKey);
    if (refreshToken == null || refreshToken.isBlank()) {
      return null;
    }
    if (properties.isStubMode()) {
      String token = "mydata-at-refresh-" + UUID.randomUUID();
      LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
      storeTokens(scopeKey, token, refreshToken, expiresAt);
      return token;
    }
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, String> body =
        Map.of(
            "grant_type",
            "refresh_token",
            "refresh_token",
            refreshToken,
            "client_id",
            properties.getClientId(),
            "client_secret",
            properties.getClientSecret());
    MyDataTokenResponse response =
        restTemplate.postForObject(
            properties.getRefreshUrl(), new HttpEntity<>(body, headers), MyDataTokenResponse.class);
    if (response == null || response.getAccessToken() == null) {
      return null;
    }
    String newRefresh =
        response.getRefreshToken() != null && !response.getRefreshToken().isBlank()
            ? response.getRefreshToken()
            : refreshToken;
    LocalDateTime expiresAt = toExpiresAt(response.getExpiresIn());
    storeTokens(scopeKey, response.getAccessToken(), newRefresh, expiresAt);
    return response.getAccessToken();
  }

  private MyDataTokenResponse fetchTokenResponse(String code) {
    if (properties.isStubMode()) {
      MyDataTokenResponse response = new MyDataTokenResponse();
      response.setAccessToken("mydata-at-" + UUID.randomUUID());
      response.setRefreshToken("mydata-rt-" + UUID.randomUUID());
      response.setExpiresIn(3600L);
      return response;
    }
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, String> body =
        Map.of(
            "grant_type", "authorization_code",
            "client_id", properties.getClientId(),
            "client_secret", properties.getClientSecret(),
            "redirect_uri", properties.getRedirectUri(),
            "code", code);
    return restTemplate.postForObject(
        properties.getTokenUrl(), new HttpEntity<>(body, headers), MyDataTokenResponse.class);
  }

  private static LocalDateTime toExpiresAt(Long expiresInSeconds) {
    if (expiresInSeconds == null) {
      return LocalDateTime.now().plusHours(1);
    }
    return LocalDateTime.ofInstant(
        Instant.ofEpochMilli(System.currentTimeMillis() + (expiresInSeconds * 1000)),
        ZoneId.systemDefault());
  }

  private void storeTokens(
      String scopeKey, String accessToken, String refreshToken, LocalDateTime expiresAt) {
    tokenStore.put(scopeKey, accessToken);
    if (refreshToken != null && !refreshToken.isBlank()) {
      tokenStore.put(REFRESH_PREFIX + scopeKey, refreshToken);
    }
    tokenStore.put(
        EXPIRES_PREFIX + scopeKey,
        String.valueOf(expiresAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
  }
}
