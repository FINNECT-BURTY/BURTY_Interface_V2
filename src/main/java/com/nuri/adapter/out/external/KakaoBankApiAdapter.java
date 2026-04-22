package com.nuri.adapter.out.external;

import com.nuri.application.port.out.KakaoBankPort;
import com.nuri.config.ExternalFinanceProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
public class KakaoBankApiAdapter implements KakaoBankPort {
    private final RestTemplate restTemplate;
    private final ExternalFinanceProperties properties;

    public KakaoBankApiAdapter(RestTemplate restTemplate, ExternalFinanceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public Map<String, Object> transfer(String userId, String toAccount, long amount) {
        if (!properties.isStubMode()) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("X-API-KEY", properties.getKakaoApiKey());
            Map<String, Object> body = Map.of("userId", userId, "toAccount", toAccount, "amount", amount);
            Map response = restTemplate.postForObject(properties.getKakaoTransferUrl(), new HttpEntity<>(body, headers), Map.class);
            if (response != null) return response;
        }
        return Map.of(
                "provider", "KAKAO_BANK",
                "transactionId", UUID.randomUUID().toString(),
                "userId", userId,
                "toAccount", toAccount,
                "amount", amount,
                "status", "ACCEPTED"
        );
    }
}
