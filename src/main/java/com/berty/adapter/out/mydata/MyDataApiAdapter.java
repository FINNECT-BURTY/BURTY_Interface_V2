package com.berty.adapter.out.mydata;

import com.berty.application.port.out.MyDataPort;
import com.berty.application.port.out.MyDataOAuthPort;
import com.berty.adapter.out.mydata.dto.MyDataAssetResponse;
import com.berty.config.MyDataProperties;
import com.berty.domain.model.AssetSnapshot;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

@Component
public class MyDataApiAdapter implements MyDataPort {
    private final MyDataOAuthPort myDataOAuthPort;
    private final RestTemplate restTemplate;
    private final MyDataProperties properties;

    public MyDataApiAdapter(MyDataOAuthPort myDataOAuthPort, RestTemplate restTemplate, MyDataProperties properties) {
        this.myDataOAuthPort = myDataOAuthPort;
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public AssetSnapshot fetchAssetSnapshot(String userId) {
        String accessToken = myDataOAuthPort.findAccessToken(userId);
        if (accessToken == null || accessToken.isBlank()) {
            // OAuth 연동 전에는 샌드박스 기본값을 반환, 실제 운영 시 예외 전환
            return new AssetSnapshot(320_000_000, 3_200_000, 12.4);
        }
        if (properties.isStubMode()) {
            return new AssetSnapshot(320_000_000, 3_200_000, 12.4);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        int maxAttempts = Math.max(1, properties.getRetryCount() + 1);
        RestClientException lastError = null;
        ResponseEntity<MyDataAssetResponse> response = null;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                response = restTemplate.exchange(
                        properties.getAssetUrl() + "?userId=" + userId,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        MyDataAssetResponse.class
                );
                lastError = null;
                break;
            } catch (RestClientException e) {
                lastError = e;
                String refreshed = myDataOAuthPort.refreshAccessToken(userId);
                if (refreshed == null || refreshed.isBlank()) {
                    break;
                }
                headers.setBearerAuth(refreshed);
            }
        }
        if (lastError != null) {
            throw lastError;
        }
        MyDataAssetResponse body = response != null ? response.getBody() : null;
        if (body == null) {
            return new AssetSnapshot(320_000_000, 3_200_000, 12.4);
        }
        double totalAsset = body.getTotalAsset() == null ? 320_000_000d : body.getTotalAsset();
        double monthlySpend = body.getMonthlySpend() == null ? 3_200_000d : body.getMonthlySpend();
        double volatility = body.getVolatilityPercent() == null ? 12.4d : body.getVolatilityPercent();
        return new AssetSnapshot(totalAsset, monthlySpend, volatility);
    }
}
