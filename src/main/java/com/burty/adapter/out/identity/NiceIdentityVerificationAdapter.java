package com.burty.adapter.out.identity;

import com.burty.application.port.out.identity.IdentityVerificationPort;
import com.burty.config.IdentityProperties;
import com.burty.core.constant.AppMessages;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(prefix = "burty.identity", name = "stub-mode", havingValue = "false")
@ConditionalOnProperty(
    prefix = "burty.identity",
    name = "provider",
    havingValue = "NICE",
    matchIfMissing = true)
public class NiceIdentityVerificationAdapter implements IdentityVerificationPort {

  private static final Logger log = LoggerFactory.getLogger(NiceIdentityVerificationAdapter.class);

  private final IdentityProperties properties;
  private final RestTemplate restTemplate;

  public NiceIdentityVerificationAdapter(IdentityProperties properties, RestTemplate restTemplate) {
    this.properties = properties;
    this.restTemplate = restTemplate;
  }

  @Override
  @SuppressWarnings("unchecked")
  public IdentityVerificationResult verify(IdentityVerificationRequest request) {
    if (!properties.getNice().isConfigured()) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE, "NICE 본인확인 설정(siteCode/sitePassword)이 필요합니다.");
    }
    if (request.name() == null
        || request.name().isBlank()
        || request.phone() == null
        || request.phone().isBlank()) {
      return new IdentityVerificationResult(false, null, null, AppMessages.Identity.VERIFY_FAILED);
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, Object> body =
        Map.of(
            "siteCode", properties.getNice().getSiteCode(),
            "sitePassword", properties.getNice().getSitePassword(),
            "name", request.name(),
            "phone", request.phone(),
            "birthDate", request.birthDate() != null ? request.birthDate() : "",
            "carrier", request.carrier() != null ? request.carrier() : "");

    try {
      Map<String, Object> response =
          restTemplate.postForObject(
              properties.getNice().getVerifyUrl(), new HttpEntity<>(body, headers), Map.class);
      if (response == null) {
        return new IdentityVerificationResult(
            false, null, null, AppMessages.Identity.VERIFY_FAILED);
      }
      boolean verified = Boolean.TRUE.equals(response.get("verified"));
      String ci = response.get("ci") != null ? String.valueOf(response.get("ci")) : null;
      String di = response.get("di") != null ? String.valueOf(response.get("di")) : null;
      String message =
          verified ? AppMessages.Identity.VERIFY_SUCCESS : AppMessages.Identity.VERIFY_FAILED;
      return new IdentityVerificationResult(verified, ci, di, message);
    } catch (RestClientException e) {
      log.error("NICE identity verification failed: {}", e.getMessage());
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "NICE 본인확인 API 호출 실패");
    }
  }
}
