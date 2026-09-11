/**
 *
 *
 * <pre>
 * <b>Description  : 설정 설정 프로퍼티 (MyDataProperties)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.config
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
package com.burty.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "burty.mydata")
public class MyDataProperties {
  private String clientId = "burty-client";
  private String clientSecret = "burty-secret";
  private String authorizeUrl = "https://sandbox.mydata.local/oauth2/authorize";
  private String tokenUrl = "https://sandbox.mydata.local/oauth2/token";
  private String refreshUrl = "https://sandbox.mydata.local/oauth2/token";

  /** 은행 정보제공자 표준 API 의 기본 주소. 경로(/v1/bank/...)는 어댑터가 붙인다. */
  private String apiBaseUrl = "https://sandbox.mydata.local";

  private String redirectUri = "http://localhost:8080/api/v1/mydata/oauth/callback";
  private String scope = "asset.read transfer.read";
  private boolean stubMode = true;
  private int retryCount = 2;

  /** 토큰 폐기(개별인증) — 철회·해제 때 정보제공자에도 무효화를 요청한다. */
  private String revokeUrl = "https://sandbox.mydata.local/oauth/2.0/revoke";

  /** 우리(마이데이터사업자)의 기관코드 10자리. 표준 API 거래고유번호 앞자리에 쓴다. */
  private String orgCode = "BURTYMYD01";

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public String getAuthorizeUrl() {
    return authorizeUrl;
  }

  public void setAuthorizeUrl(String authorizeUrl) {
    this.authorizeUrl = authorizeUrl;
  }

  public String getTokenUrl() {
    return tokenUrl;
  }

  public void setTokenUrl(String tokenUrl) {
    this.tokenUrl = tokenUrl;
  }

  public String getApiBaseUrl() {
    return apiBaseUrl;
  }

  public void setApiBaseUrl(String apiBaseUrl) {
    this.apiBaseUrl = apiBaseUrl;
  }

  public String getRefreshUrl() {
    return refreshUrl;
  }

  public void setRefreshUrl(String refreshUrl) {
    this.refreshUrl = refreshUrl;
  }

  public String getRedirectUri() {
    return redirectUri;
  }

  public void setRedirectUri(String redirectUri) {
    this.redirectUri = redirectUri;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public boolean isStubMode() {
    return stubMode;
  }

  public void setStubMode(boolean stubMode) {
    this.stubMode = stubMode;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(int retryCount) {
    this.retryCount = retryCount;
  }

  public String getRevokeUrl() {
    return revokeUrl;
  }

  public void setRevokeUrl(String revokeUrl) {
    this.revokeUrl = revokeUrl;
  }

  public String getOrgCode() {
    return orgCode;
  }

  public void setOrgCode(String orgCode) {
    this.orgCode = orgCode;
  }
}
