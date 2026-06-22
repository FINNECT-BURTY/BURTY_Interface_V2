/**
 *
 *
 * <pre>
 * <b>Description  : 마이데이터 외부 연동 어댑터 (MyDataApiAdapter)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.out.mydata
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
package com.burty.adapter.out.mydata;

import com.burty.adapter.out.http.ResilientHttpExecutor;
import com.burty.adapter.out.mydata.dto.MyDataAssetResponse;
import com.burty.application.port.out.mydata.MyDataOAuthPort;
import com.burty.application.port.out.mydata.MyDataPort;
import com.burty.application.service.mydata.MyDataTokenHydrationService;
import com.burty.config.MyDataProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.asset.model.AssetSnapshot;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class MyDataApiAdapter implements MyDataPort {
  private static final String DEFAULT_INSTITUTION = "MYDATA";

  private final MyDataOAuthPort myDataOAuthPort;
  private final MyDataTokenHydrationService tokenHydrationService;
  private final RestTemplate restTemplate;
  private final MyDataProperties properties;
  private final ResilientHttpExecutor resilientHttpExecutor;

  public MyDataApiAdapter(
      MyDataOAuthPort myDataOAuthPort,
      MyDataTokenHydrationService tokenHydrationService,
      RestTemplate restTemplate,
      MyDataProperties properties,
      ResilientHttpExecutor resilientHttpExecutor) {
    this.myDataOAuthPort = myDataOAuthPort;
    this.tokenHydrationService = tokenHydrationService;
    this.restTemplate = restTemplate;
    this.properties = properties;
    this.resilientHttpExecutor = resilientHttpExecutor;
  }

  @Override
  public AssetSnapshot fetchAssetSnapshot(String userId) {
    if (properties.isStubMode()) {
      return sandboxSnapshot();
    }

    tokenHydrationService.hydrate(userId, DEFAULT_INSTITUTION);
    String scopeKey = MyDataOAuthPort.scopeKey(userId, DEFAULT_INSTITUTION);
    String accessToken = myDataOAuthPort.findAccessToken(scopeKey);
    if (accessToken == null || accessToken.isBlank()) {
      throw new BusinessException(
          ErrorCode.DATA_NOT_FOUND, "MyData 연동 토큰이 없습니다. 먼저 기관 연동을 완료해주세요.");
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    return resilientHttpExecutor.execute(
        "mydata", () -> fetchAssetSnapshotInternal(userId, headers, scopeKey));
  }

  private AssetSnapshot fetchAssetSnapshotInternal(
      String userId, HttpHeaders headers, String scopeKey) {
    int maxAttempts = Math.max(1, properties.getRetryCount() + 1);
    RestClientException lastError = null;
    ResponseEntity<MyDataAssetResponse> response = null;
    for (int attempt = 0; attempt < maxAttempts; attempt++) {
      try {
        response =
            restTemplate.exchange(
                properties.getAssetUrl() + "?userId=" + userId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                MyDataAssetResponse.class);
        lastError = null;
        break;
      } catch (RestClientException e) {
        lastError = e;
        String refreshed = myDataOAuthPort.refreshAccessToken(scopeKey);
        if (refreshed == null || refreshed.isBlank()) {
          break;
        }
        headers.setBearerAuth(refreshed);
      }
    }
    if (lastError != null) {
      throw new BusinessException(
          ErrorCode.EXTERNAL_API_ERROR, "MyData 자산 조회에 실패했습니다: " + lastError.getMessage());
    }
    MyDataAssetResponse body = response != null ? response.getBody() : null;
    if (body == null) {
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "MyData 자산 응답이 비어 있습니다.");
    }
    double totalAsset = body.getTotalAsset() == null ? 0d : body.getTotalAsset();
    double monthlySpend = body.getMonthlySpend() == null ? 0d : body.getMonthlySpend();
    double volatility = body.getVolatilityPercent() == null ? 0d : body.getVolatilityPercent();
    return new AssetSnapshot(totalAsset, monthlySpend, volatility);
  }

  private static AssetSnapshot sandboxSnapshot() {
    return new AssetSnapshot(320_000_000, 3_200_000, 12.4);
  }
}
