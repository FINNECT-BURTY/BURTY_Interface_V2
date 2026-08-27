package com.burty.adapter.out.identity;

import com.burty.application.port.out.identity.IdentityVerificationPort;
import com.burty.config.IdentityProperties;
import com.burty.core.config.HttpClientFactory;
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
@ConditionalOnProperty(prefix = "burty.identity", name = "provider", havingValue = "KCB")
public class KcbIdentityVerificationAdapter implements IdentityVerificationPort {

  private static final Logger log = LoggerFactory.getLogger(KcbIdentityVerificationAdapter.class);

  private final IdentityProperties properties;
  private final RestTemplate restTemplate;

  public KcbIdentityVerificationAdapter(
      IdentityProperties properties, HttpClientFactory httpClientFactory) {
    this.properties = properties;
    this.restTemplate = httpClientFactory.restTemplate("identity-kcb", properties.getTimeoutMs());
  }

  @Override
  @SuppressWarnings("unchecked")
  public IdentityVerificationResult verify(IdentityVerificationRequest request) {
    if (!properties.getKcb().isConfigured()) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE, "KCB 본인확인 설정(cpCode/licenseKey)이 필요합니다.");
    }
    if (request.name() == null
        || request.name().isBlank()
        || request.phone() == null
        || request.phone().isBlank()) {
      return new IdentityVerificationResult(false, null, null, AppMessages.Identity.VERIFY_FAILED);
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-KCB-LICENSE", properties.getKcb().getLicenseKey());
    Map<String, Object> body =
        Map.of(
            "cpCode", properties.getKcb().getCpCode(),
            "name", request.name(),
            "phone", request.phone(),
            "birthDate", request.birthDate() != null ? request.birthDate() : "",
            "carrier", request.carrier() != null ? request.carrier() : "");

    try {
      Map<String, Object> response =
          restTemplate.postForObject(
              properties.getKcb().getVerifyUrl(), new HttpEntity<>(body, headers), Map.class);
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
      log.error("KCB identity verification failed: {}", e.getMessage());
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "KCB 본인확인 API 호출 실패");
    }
  }
}
