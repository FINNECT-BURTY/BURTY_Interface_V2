/**
 *
 *
 * <pre>
 * <b>Description  : 외부연동 외부 연동 어댑터 (AbstractBankTransferAdapter)</b>
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

import com.burty.config.ExternalFinanceProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

abstract class AbstractBankTransferAdapter {

  protected final RestTemplate restTemplate;
  protected final ExternalFinanceProperties properties;

  protected AbstractBankTransferAdapter(
      RestTemplate restTemplate, ExternalFinanceProperties properties) {
    this.restTemplate = restTemplate;
    this.properties = properties;
  }

  protected Map<String, Object> transfer(
      String provider,
      String transferUrl,
      String apiKey,
      String userId,
      String toAccount,
      long amount) {
    if (properties.isStubMode()) {
      return stubTransfer(provider, userId, toAccount, amount);
    }
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.add("X-API-KEY", apiKey);
    Map<String, Object> body = Map.of("userId", userId, "toAccount", toAccount, "amount", amount);
    @SuppressWarnings("unchecked")
    Map<String, Object> response =
        restTemplate.postForObject(transferUrl, new HttpEntity<>(body, headers), Map.class);
    if (response == null) {
      throw new BusinessException(
          ErrorCode.EXTERNAL_API_ERROR, provider + " 이체 API가 빈 응답을 반환했습니다. userId=" + userId);
    }
    return response;
  }

  private Map<String, Object> stubTransfer(
      String provider, String userId, String toAccount, long amount) {
    return Map.of(
        "provider",
        provider,
        "transactionId",
        UUID.randomUUID().toString(),
        "userId",
        userId,
        "toAccount",
        toAccount,
        "amount",
        amount,
        "status",
        "ACCEPTED");
  }
}
