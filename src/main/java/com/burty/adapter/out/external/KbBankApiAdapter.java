package com.burty.adapter.out.external;

import com.burty.application.port.out.KbBankPort;
import com.burty.config.ExternalFinanceProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
public class KbBankApiAdapter implements KbBankPort {
    private final RestTemplate restTemplate;
    private final ExternalFinanceProperties properties;

    public KbBankApiAdapter(RestTemplate restTemplate, ExternalFinanceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public Map<String, Object> transfer(String userId, String toAccount, long amount) {
        if (!properties.isStubMode()) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("X-API-KEY", properties.getKbApiKey());
            Map<String, Object> body = Map.of("userId", userId, "toAccount", toAccount, "amount", amount);
            Map response = restTemplate.postForObject(properties.getKbTransferUrl(), new HttpEntity<>(body, headers), Map.class);
            if (response != null) return response;
        }
        return Map.of(
                "provider", "KB_BANK",
                "transactionId", UUID.randomUUID().toString(),
                "userId", userId,
                "toAccount", toAccount,
                "amount", amount,
                "status", "ACCEPTED"
        );
    }
}
