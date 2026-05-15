package com.burty.adapter.out.external;

import com.burty.application.port.out.OpenBankingPort;
import com.burty.adapter.out.store.TokenStore;
import com.burty.config.ExternalFinanceProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class OpenBankingApiAdapter implements OpenBankingPort {
    private static final String ACCESS_PREFIX = "openbanking:access:";
    private static final String REFRESH_PREFIX = "openbanking:refresh:";

    private final RestTemplate restTemplate;
    private final ExternalFinanceProperties properties;
    private final TokenStore tokenStore;

    public OpenBankingApiAdapter(RestTemplate restTemplate, ExternalFinanceProperties properties, TokenStore tokenStore) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.tokenStore = tokenStore;
    }

    @Override
    public Map<String, Object> getAccounts(String userId) {
        if (!properties.isStubMode()) {
            Map<String, Object> response = executeWithRetry(userId, token ->
                    restTemplate.exchange(
                            properties.getOpenBankingAccountsUrl() + "?userId=" + userId,
                            HttpMethod.GET,
                            new HttpEntity<>(buildHeaders(userId, token)),
                            new ParameterizedTypeReference<Map<String, Object>>() {}
                    ).getBody()
            );
            if (response != null) return response;
        }
        return Map.of(
                "provider", "OPEN_BANKING",
                "userId", userId,
                "accounts", List.of(
                        Map.of("fintechUseNum", "199001234567890123456789", "bankName", "DemoBank", "accountMasked", "123-****-8901")
                )
        );
    }

    @Override
    public Map<String, Object> getBalance(String userId, String fintechUseNum) {
        if (!properties.isStubMode()) {
            Map<String, Object> response = executeWithRetry(userId, token ->
                    restTemplate.exchange(
                            properties.getOpenBankingBalanceUrl() + "?fintechUseNum=" + fintechUseNum,
                            HttpMethod.GET,
                            new HttpEntity<>(buildHeaders(userId, token)),
                            new ParameterizedTypeReference<Map<String, Object>>() {}
                    ).getBody()
            );
            if (response != null) return response;
        }
        return Map.of(
                "provider", "OPEN_BANKING",
                "userId", userId,
                "fintechUseNum", fintechUseNum,
                "balance", 2500000L,
                "currency", "KRW"
        );
    }

    @Override
    public Map<String, Object> getTransactions(String userId, String fintechUseNum) {
        if (!properties.isStubMode()) {
            Map<String, Object> response = executeWithRetry(userId, token ->
                    restTemplate.exchange(
                            properties.getOpenBankingTransactionsUrl() + "?fintechUseNum=" + fintechUseNum,
                            HttpMethod.GET,
                            new HttpEntity<>(buildHeaders(userId, token)),
                            new ParameterizedTypeReference<Map<String, Object>>() {}
                    ).getBody()
            );
            if (response != null) return response;
        }
        return Map.of(
                "provider", "OPEN_BANKING",
                "userId", userId,
                "fintechUseNum", fintechUseNum,
                "transactions", List.of(
                        Map.of("type", "DEPOSIT", "amount", 120000L, "memo", "salary"),
                        Map.of("type", "WITHDRAWAL", "amount", 45000L, "memo", "coffee")
                )
        );
    }

    @Override
    public Map<String, Object> transfer(String userId, String fromAccount, String toAccount, long amount, String idempotencyKey) {
        if (!properties.isStubMode()) {
            Map<String, Object> response = executeWithRetry(userId, token -> {
                HttpHeaders headers = buildHeaders(userId, token);
                headers.setContentType(MediaType.APPLICATION_JSON);
                if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                    headers.add("X-Idempotency-Key", idempotencyKey);
                }
                Map<String, Object> body = Map.of(
                        "fromAccount", fromAccount,
                        "toAccount", toAccount,
                        "amount", amount,
                        "bankTranId", UUID.randomUUID().toString()
                );
                return restTemplate.exchange(
                        properties.getOpenBankingTransferUrl(),
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        new ParameterizedTypeReference<Map<String, Object>>() {}
                ).getBody();
            });
            if (response != null) return response;
        }
        return Map.of(
                "provider", "OPEN_BANKING",
                "transactionId", UUID.randomUUID().toString(),
                "userId", userId,
                "fromAccount", fromAccount,
                "toAccount", toAccount,
                "amount", amount,
                "idempotencyKey", idempotencyKey,
                "status", "ACCEPTED"
        );
    }

    private HttpHeaders buildHeaders(String userId, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("X-CLIENT-ID", properties.getOpenBankingClientId());
        headers.add("X-USER-ID", userId);
        return headers;
    }

    private Map<String, Object> executeWithRetry(String userId, Function<String, Map<String, Object>> call) {
        int maxAttempts = Math.max(1, properties.getOpenBankingRetryCount() + 1);
        String token = resolveAccessToken(userId);
        RestClientException lastError = null;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                return call.apply(token);
            } catch (RestClientResponseException e) {
                lastError = e;
                if (e.getStatusCode().value() == 401) {
                    String refreshed = refreshAccessToken(userId);
                    if (refreshed == null || refreshed.isBlank()) {
                        break;
                    }
                    token = refreshed;
                    continue;
                }
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "OpenBanking API 오류: HTTP " + e.getStatusCode().value(), e);
            } catch (RestClientException e) {
                lastError = e;
            }
        }
        throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "OpenBanking API 호출 실패", lastError);
    }

    private String resolveAccessToken(String userId) {
        String token = tokenStore.get(ACCESS_PREFIX + userId);
        if (token != null && !token.isBlank()) {
            return token;
        }
        return properties.getOpenBankingAccessToken();
    }

    private String refreshAccessToken(String userId) {
        String refreshToken = tokenStore.get(REFRESH_PREFIX + userId);
        if (refreshToken == null || refreshToken.isBlank()) {
            refreshToken = properties.getOpenBankingRefreshToken();
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = Map.of(
                "grant_type", "refresh_token",
                "refresh_token", refreshToken,
                "client_id", properties.getOpenBankingClientId(),
                "client_secret", properties.getOpenBankingClientSecret()
        );
        Map<String, Object> response = restTemplate.exchange(
                properties.getOpenBankingTokenUrl(),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody();
        if (response == null) {
            return null;
        }
        Object newAccessToken = response.get("access_token");
        if (newAccessToken instanceof String token && !token.isBlank()) {
            tokenStore.put(ACCESS_PREFIX + userId, token);
            Object newRefreshToken = response.get("refresh_token");
            if (newRefreshToken instanceof String rt && !rt.isBlank()) {
                tokenStore.put(REFRESH_PREFIX + userId, rt);
            }
            return token;
        }
        return null;
    }
}
