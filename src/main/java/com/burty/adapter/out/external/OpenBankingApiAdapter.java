/**
 *
 *
 * <pre>
 * <b>Description  : 외부연동 외부 연동 어댑터 (OpenBankingApiAdapter)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.out.external
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
package com.burty.adapter.out.external;

import com.burty.adapter.out.http.ResilientHttpExecutor;
import com.burty.adapter.out.store.TokenStore;
import com.burty.application.port.out.bank.OpenBankingPort;
import com.burty.application.port.out.bank.TransferStatus;
import com.burty.config.ExternalFinanceProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.core.exception.ExternalCallUnresolvedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OpenBankingApiAdapter implements OpenBankingPort {
  private static final String ACCESS_PREFIX = "openbanking:access:";
  private static final String REFRESH_PREFIX = "openbanking:refresh:";

  private final RestTemplate restTemplate;
  private final ExternalFinanceProperties properties;
  private final TokenStore tokenStore;
  private final ResilientHttpExecutor resilientHttpExecutor;

  public OpenBankingApiAdapter(
      RestTemplate restTemplate,
      ExternalFinanceProperties properties,
      TokenStore tokenStore,
      ResilientHttpExecutor resilientHttpExecutor) {
    this.restTemplate = restTemplate;
    this.properties = properties;
    this.tokenStore = tokenStore;
    this.resilientHttpExecutor = resilientHttpExecutor;
  }

  @Override
  public Map<String, Object> getAccounts(String userId) {
    if (properties.isStubMode()) {
      return stubAccounts(userId);
    }
    return requireResponse(
        executeWithRetry(
            userId,
            token ->
                restTemplate
                    .exchange(
                        UriComponentsBuilder.fromUriString(properties.getOpenBankingAccountsUrl())
                            .queryParam("userId", userId)
                            .build(true)
                            .toUriString(),
                        HttpMethod.GET,
                        new HttpEntity<>(buildHeaders(userId, token)),
                        new ParameterizedTypeReference<Map<String, Object>>() {})
                    .getBody()),
        "accounts");
  }

  @Override
  public Map<String, Object> getBalance(String userId, String fintechUseNum) {
    if (properties.isStubMode()) {
      return stubBalance(userId, fintechUseNum);
    }
    return requireResponse(
        executeWithRetry(
            userId,
            token ->
                restTemplate
                    .exchange(
                        UriComponentsBuilder.fromUriString(properties.getOpenBankingBalanceUrl())
                            .queryParam("fintechUseNum", fintechUseNum)
                            .build(true)
                            .toUriString(),
                        HttpMethod.GET,
                        new HttpEntity<>(buildHeaders(userId, token)),
                        new ParameterizedTypeReference<Map<String, Object>>() {})
                    .getBody()),
        "balance");
  }

  @Override
  public Map<String, Object> getTransactions(String userId, String fintechUseNum) {
    if (properties.isStubMode()) {
      return stubTransactions(userId, fintechUseNum);
    }
    return requireResponse(
        executeWithRetry(
            userId,
            token ->
                restTemplate
                    .exchange(
                        UriComponentsBuilder.fromUriString(
                                properties.getOpenBankingTransactionsUrl())
                            .queryParam("fintechUseNum", fintechUseNum)
                            .build(true)
                            .toUriString(),
                        HttpMethod.GET,
                        new HttpEntity<>(buildHeaders(userId, token)),
                        new ParameterizedTypeReference<Map<String, Object>>() {})
                    .getBody()),
        "transactions");
  }

  @Override
  public Map<String, Object> transfer(
      String userId, String fromAccount, String toAccount, long amount, String idempotencyKey) {
    if (properties.isStubMode()) {
      return stubTransfer(userId, fromAccount, toAccount, amount, idempotencyKey);
    }
    // 이체는 멱등하지 않은 쓰기다. 재시도는 은행이 같은 거래번호를 중복 거래로 인식할 때만 안전하므로
    // bankTranId 를 멱등키에서 결정론적으로 도출한다. (기존에는 시도마다 랜덤 UUID 라 은행이 중복을 못 잡았다.)
    String bankTranId = deterministicBankTranId(userId, idempotencyKey);
    return requireResponse(
        executeMutating(
            "transfer",
            userId,
            token -> {
              HttpHeaders headers = buildHeaders(userId, token);
              headers.setContentType(MediaType.APPLICATION_JSON);
              if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                headers.add("X-Idempotency-Key", idempotencyKey);
              }
              Map<String, Object> body =
                  Map.of(
                      "fromAccount", fromAccount,
                      "toAccount", toAccount,
                      "amount", amount,
                      "bankTranId", bankTranId);
              return restTemplate
                  .exchange(
                      properties.getOpenBankingTransferUrl(),
                      HttpMethod.POST,
                      new HttpEntity<>(body, headers),
                      new ParameterizedTypeReference<Map<String, Object>>() {})
                  .getBody();
            }),
        "transfer");
  }

  @Override
  public TransferStatus getTransferStatus(String userId, String idempotencyKey) {
    String bankTranId = deterministicBankTranId(userId, idempotencyKey);
    if (properties.isStubMode()) {
      return TransferStatus.completed(bankTranId);
    }
    try {
      Map<String, Object> response =
          executeWithRetry(
              userId,
              token ->
                  restTemplate
                      .exchange(
                          UriComponentsBuilder.fromUriString(
                                  properties.getOpenBankingTransferStatusUrl())
                              .queryParam("bank_tran_id", bankTranId)
                              .build(true)
                              .toUriString(),
                          HttpMethod.GET,
                          new HttpEntity<>(buildHeaders(userId, token)),
                          new ParameterizedTypeReference<Map<String, Object>>() {})
                      .getBody());
      return mapTransferStatus(response, bankTranId);
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().value() == 404) {
        return TransferStatus.notFound();
      }
      return TransferStatus.unresolved("HTTP " + e.getStatusCode().value());
    } catch (RuntimeException e) {
      return TransferStatus.unresolved(e.getMessage());
    }
  }

  private static TransferStatus mapTransferStatus(Map<String, Object> response, String bankTranId) {
    if (response == null) {
      return TransferStatus.unresolved("빈 응답");
    }
    String code = String.valueOf(response.getOrDefault("rsp_code", ""));
    String status = String.valueOf(response.getOrDefault("status", "")).toUpperCase();
    if ("A0000".equals(code) || "COMPLETED".equals(status) || "SUCCESS".equals(status)) {
      Object txnId = response.get("bank_tran_id");
      return TransferStatus.completed(txnId != null ? String.valueOf(txnId) : bankTranId);
    }
    if ("PENDING".equals(status) || "PROCESSING".equals(status)) {
      return TransferStatus.pending();
    }
    if ("NOT_FOUND".equals(status)) {
      return TransferStatus.notFound();
    }
    if (!status.isBlank() || !code.isBlank()) {
      return TransferStatus.rejected(("".equals(code) ? status : code));
    }
    return TransferStatus.unresolved("판단 불가 응답");
  }

  /** 멱등키에서 은행 거래고유번호를 결정론적으로 도출한다. 같은 이체 요청은 몇 번을 재시도해도 같은 번호를 쓰므로, 은행 측 중복 방지가 실제로 동작한다. */
  private static String deterministicBankTranId(String userId, String idempotencyKey) {
    String seed = userId + "|" + (idempotencyKey == null ? "" : idempotencyKey);
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(seed.getBytes(StandardCharsets.UTF_8));
      return "B" + HexFormat.of().formatHex(hash).substring(0, 19).toUpperCase();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 미지원", e);
    }
  }

  /**
   * 상태를 바꾸는 호출 전용 실행기.
   *
   * <p>조회와 달리 <b>재시도하지 않는다.</b> 그리고 응답을 받지 못한 경우 실패가 아니라 {@link ExternalCallUnresolvedException} 을
   * 던져, 호출자가 "출금됐을 수도 있는 건"으로 다루게 한다.
   */
  private Map<String, Object> executeMutating(
      String operation, String userId, Function<String, Map<String, Object>> call) {
    return resilientHttpExecutor.execute(
        "openbanking",
        () -> {
          String token = resolveAccessToken(userId);
          try {
            return call.apply(token);
          } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 401) {
              String refreshed = refreshAccessToken(userId);
              if (refreshed == null || refreshed.isBlank()) {
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "OpenBanking 인증 실패", e);
              }
              // 401 은 요청이 거절된 것이므로 출금이 발생하지 않았다. 재시도가 안전하다.
              return call.apply(refreshed);
            }
            if (status >= 500) {
              throw new ExternalCallUnresolvedException(
                  operation, "OpenBanking 서버 오류: HTTP " + status, e);
            }
            throw new BusinessException(
                ErrorCode.EXTERNAL_API_ERROR, "OpenBanking API 오류: HTTP " + status, e);
          } catch (RestClientException e) {
            // 타임아웃/IO 오류 — 은행에 도달했는지 알 수 없다.
            throw new ExternalCallUnresolvedException(
                operation, "OpenBanking 응답 확인 불가: " + e.getMessage(), e);
          }
        });
  }

  private Map<String, Object> requireResponse(Map<String, Object> response, String operation) {
    if (response == null) {
      throw new BusinessException(
          ErrorCode.EXTERNAL_API_ERROR,
          "OpenBanking " + operation + " API returned empty response");
    }
    return response;
  }

  private Map<String, Object> stubAccounts(String userId) {
    return Map.of(
        "provider",
        "OPEN_BANKING",
        "userId",
        userId,
        "accounts",
        List.of(
            Map.of(
                "fintechUseNum",
                "199001234567890123456789",
                "bankName",
                "DemoBank",
                "accountMasked",
                "123-****-8901")));
  }

  private Map<String, Object> stubBalance(String userId, String fintechUseNum) {
    return Map.of(
        "provider",
        "OPEN_BANKING",
        "userId",
        userId,
        "fintechUseNum",
        fintechUseNum,
        "balance",
        2500000L,
        "currency",
        "KRW");
  }

  private Map<String, Object> stubTransactions(String userId, String fintechUseNum) {
    return Map.of(
        "provider",
        "OPEN_BANKING",
        "userId",
        userId,
        "fintechUseNum",
        fintechUseNum,
        "transactions",
        List.of(
            Map.of("type", "DEPOSIT", "amount", 120000L, "memo", "salary"),
            Map.of("type", "WITHDRAWAL", "amount", 45000L, "memo", "coffee")));
  }

  private Map<String, Object> stubTransfer(
      String userId, String fromAccount, String toAccount, long amount, String idempotencyKey) {
    return Map.of(
        "provider", "OPEN_BANKING",
        "transactionId", UUID.randomUUID().toString(),
        "userId", userId,
        "fromAccount", fromAccount,
        "toAccount", toAccount,
        "amount", amount,
        "idempotencyKey", idempotencyKey,
        "status", "ACCEPTED");
  }

  private HttpHeaders buildHeaders(String userId, String accessToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + accessToken);
    headers.add("X-CLIENT-ID", properties.getOpenBankingClientId());
    headers.add("X-USER-ID", userId);
    return headers;
  }

  private Map<String, Object> executeWithRetry(
      String userId, Function<String, Map<String, Object>> call) {
    return resilientHttpExecutor.execute(
        "openbanking", () -> executeWithRetryInternal(userId, call));
  }

  private Map<String, Object> executeWithRetryInternal(
      String userId, Function<String, Map<String, Object>> call) {
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
        throw new BusinessException(
            ErrorCode.EXTERNAL_API_ERROR,
            "OpenBanking API 오류: HTTP " + e.getStatusCode().value(),
            e);
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
    Map<String, String> body =
        Map.of(
            "grant_type",
            "refresh_token",
            "refresh_token",
            refreshToken,
            "client_id",
            properties.getOpenBankingClientId(),
            "client_secret",
            properties.getOpenBankingClientSecret());
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
