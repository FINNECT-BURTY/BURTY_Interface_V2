package com.berty.adapter.out.mydata;

import com.berty.application.port.out.MyDataOAuthPort;
import com.berty.config.MyDataProperties;
import com.berty.adapter.out.mydata.dto.MyDataTokenResponse;
import com.berty.adapter.out.store.TokenStore;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.Map;

@Component
public class MyDataOAuthAdapter implements MyDataOAuthPort {
    private static final String REFRESH_PREFIX = "refresh:";
    private static final String EXPIRES_PREFIX = "expires:";
    private final RestTemplate restTemplate;
    private final MyDataProperties properties;
    private final TokenStore tokenStore;

    public MyDataOAuthAdapter(RestTemplate restTemplate, MyDataProperties properties, TokenStore tokenStore) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.tokenStore = tokenStore;
    }

    @Override
    public String buildAuthorizeUrl(String userId) {
        return properties.getAuthorizeUrl()
                + "?response_type=code&client_id=" + properties.getClientId()
                + "&redirect_uri=" + properties.getRedirectUri()
                + "&scope=" + properties.getScope()
                + "&state=" + userId;
    }

    @Override
    public String exchangeCodeForAccessToken(String userId, String code) {
        if (code == null || code.isBlank()) return null;
        MyDataTokenResponse response;
        if (properties.isStubMode()) {
            response = new MyDataTokenResponse();
            response.setAccessToken("mydata-at-" + UUID.randomUUID());
            response.setRefreshToken("mydata-rt-" + UUID.randomUUID());
            response.setExpiresIn(3L);
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> body = Map.of(
                    "grant_type", "authorization_code",
                    "client_id", properties.getClientId(),
                    "client_secret", properties.getClientSecret(),
                    "redirect_uri", properties.getRedirectUri(),
                    "code", code
            );
            response = restTemplate.postForObject(properties.getTokenUrl(), new HttpEntity<>(body, headers), MyDataTokenResponse.class);
        }
        String token = response == null ? null : response.getAccessToken();
        if (token == null || token.isBlank()) return null;
        tokenStore.put(userId, token);
        if (response != null && response.getRefreshToken() != null) {
            tokenStore.put(REFRESH_PREFIX + userId, response.getRefreshToken());
        }
        if (response != null && response.getExpiresIn() != null) {
            tokenStore.put(EXPIRES_PREFIX + userId, String.valueOf(System.currentTimeMillis() + (response.getExpiresIn() * 1000)));
        }
        return token;
    }

    @Override
    public String findAccessToken(String userId) {
        return tokenStore.get(userId);
    }

    @Override
    public LocalDateTime findTokenExpiresAt(String userId) {
        String raw = tokenStore.get(EXPIRES_PREFIX + userId);
        if (raw == null || raw.isBlank()) return null;
        try {
            long epochMillis = Long.parseLong(raw);
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String refreshAccessToken(String userId) {
        String refreshToken = tokenStore.get(REFRESH_PREFIX + userId);
        if (refreshToken == null || refreshToken.isBlank()) return null;
        if (properties.isStubMode()) {
            String token = "mydata-at-refresh-" + UUID.randomUUID();
            tokenStore.put(userId, token);
            tokenStore.put(EXPIRES_PREFIX + userId, String.valueOf(System.currentTimeMillis() + 3_000));
            return token;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = Map.of(
                "grant_type", "refresh_token",
                "refresh_token", refreshToken,
                "client_id", properties.getClientId(),
                "client_secret", properties.getClientSecret()
        );
        MyDataTokenResponse response = restTemplate.postForObject(properties.getRefreshUrl(), new HttpEntity<>(body, headers), MyDataTokenResponse.class);
        if (response == null || response.getAccessToken() == null) return null;
        tokenStore.put(userId, response.getAccessToken());
        if (response.getExpiresIn() != null) {
            tokenStore.put(EXPIRES_PREFIX + userId, String.valueOf(System.currentTimeMillis() + (response.getExpiresIn() * 1000)));
        }
        return response.getAccessToken();
    }
}
