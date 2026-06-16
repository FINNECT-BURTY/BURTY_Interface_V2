package com.burty.adapter.out.external;

import com.burty.adapter.out.store.TokenStore;
import com.burty.application.port.out.bank.OpenBankingOAuthPort;
import com.burty.config.ExternalFinanceProperties;
import com.burty.domain.mydata.model.MyDataTokenBundle;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class OpenBankingOAuthAdapter implements OpenBankingOAuthPort {

  private static final String ACCESS_PREFIX = "openbanking:access:";
  private static final String REFRESH_PREFIX = "openbanking:refresh:";

  private final RestTemplate restTemplate;
  private final ExternalFinanceProperties properties;
  private final TokenStore tokenStore;

  public OpenBankingOAuthAdapter(
      RestTemplate restTemplate, ExternalFinanceProperties properties, TokenStore tokenStore) {
    this.restTemplate = restTemplate;
    this.properties = properties;
    this.tokenStore = tokenStore;
  }

  @Override
  public String buildAuthorizeUrl(String oauthState) {
    return properties.getOpenBankingAuthorizeUrl()
        + "?response_type=code&client_id="
        + properties.getOpenBankingClientId()
        + "&redirect_uri="
        + properties.getOpenBankingRedirectUri()
        + "&scope="
        + properties.getOpenBankingScope()
        + "&state="
        + oauthState;
  }

  @Override
  public MyDataTokenBundle exchangeAuthorizationCode(String userId, String code) {
    if (code == null || code.isBlank()) {
      return null;
    }
    if (properties.isStubMode()) {
      String access = "ob-at-" + UUID.randomUUID();
      String refresh = "ob-rt-" + UUID.randomUUID();
      LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
      store(userId, access, refresh);
      return new MyDataTokenBundle(access, refresh, expiresAt);
    }
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, String> body =
        Map.of(
            "grant_type", "authorization_code",
            "client_id", properties.getOpenBankingClientId(),
            "client_secret", properties.getOpenBankingClientSecret(),
            "redirect_uri", properties.getOpenBankingRedirectUri(),
            "code", code);
    Map<String, Object> response =
        restTemplate
            .exchange(
                properties.getOpenBankingTokenUrl(),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<Map<String, Object>>() {})
            .getBody();
    if (response == null) {
      return null;
    }
    Object accessObj = response.get("access_token");
    if (!(accessObj instanceof String access) || access.isBlank()) {
      return null;
    }
    String refresh = response.get("refresh_token") instanceof String rt ? rt : "";
    store(userId, access, refresh);
    return new MyDataTokenBundle(access, refresh, LocalDateTime.now().plusHours(1));
  }

  @Override
  public boolean isLinked(String userId) {
    String token = tokenStore.get(ACCESS_PREFIX + userId);
    return token != null && !token.isBlank();
  }

  private void store(String userId, String access, String refresh) {
    tokenStore.put(ACCESS_PREFIX + userId, access);
    if (refresh != null && !refresh.isBlank()) {
      tokenStore.put(REFRESH_PREFIX + userId, refresh);
    }
  }
}
