package com.berty.adapter.out.external;

import com.berty.application.port.out.PensionPort;
import com.berty.config.ExternalFinanceProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

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
            Map response = restTemplate.exchange(
                    properties.getPensionSummaryUrl() + "?userId=" + userId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            ).getBody();
            if (response != null) return response;
        }
        return Map.of(
                "provider", "NATIONAL_PENSION",
                "userId", userId,
                "monthlyExpectedPension", 1450000,
                "status", "OK"
        );
    }
}
