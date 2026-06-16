/**
 *
 *
 * <pre>
 * <b>Description  : 외부연동 외부 연동 어댑터 (PensionApiAdapter)</b>
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

import com.burty.application.port.out.bank.PensionPort;
import com.burty.config.ExternalFinanceProperties;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PensionApiAdapter implements PensionPort {
  private final RestTemplate restTemplate;
  private final ExternalFinanceProperties properties;

  public PensionApiAdapter(RestTemplate restTemplate, ExternalFinanceProperties properties) {
    this.restTemplate = restTemplate;
    this.properties = properties;
  }

  @Override
  public Map<String, Object> getSummary(String userId) {
    if (!properties.isStubMode()) {
      HttpHeaders headers = new HttpHeaders();
      headers.add("X-API-KEY", properties.getPensionApiKey());
      Map response =
          restTemplate
              .exchange(
                  properties.getPensionSummaryUrl() + "?userId=" + userId,
                  HttpMethod.GET,
                  new HttpEntity<>(headers),
                  Map.class)
              .getBody();
      if (response != null) return response;
    }
    return Map.of(
        "provider",
        "NATIONAL_PENSION",
        "userId",
        userId,
        "monthlyExpectedPension",
        1450000,
        "status",
        "OK");
  }
}
