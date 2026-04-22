package com.nuri.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nuri.external")
public class ExternalFinanceProperties {
    private boolean stubMode = true;
    private String kakaoTransferUrl = "https://api.kakaobank.local/openbanking/transfers";
    private String kakaoApiKey = "kakao-api-key";
    private String hanaTransferUrl = "https://api.hanabank.local/openbanking/transfers";
    private String hanaApiKey = "hana-api-key";
    private String kbTransferUrl = "https://api.kbbank.local/openbanking/transfers";
    private String kbApiKey = "kb-api-key";
    private String shinhanTransferUrl = "https://api.shinhanbank.local/openbanking/transfers";
    private String shinhanApiKey = "shinhan-api-key";
    private String imTransferUrl = "https://api.imbank.local/openbanking/transfers";
    private String imApiKey = "im-api-key";
    private String pensionSummaryUrl = "https://api.pension.local/v1/summary";
    private String pensionApiKey = "pension-api-key";
    private int timeoutMs = 5000;

    public boolean isStubMode() { return stubMode; }
    public void setStubMode(boolean stubMode) { this.stubMode = stubMode; }
    public String getKakaoTransferUrl() { return kakaoTransferUrl; }
    public void setKakaoTransferUrl(String kakaoTransferUrl) { this.kakaoTransferUrl = kakaoTransferUrl; }
    public String getKakaoApiKey() { return kakaoApiKey; }
    public void setKakaoApiKey(String kakaoApiKey) { this.kakaoApiKey = kakaoApiKey; }
    public String getHanaTransferUrl() { return hanaTransferUrl; }
    public void setHanaTransferUrl(String hanaTransferUrl) { this.hanaTransferUrl = hanaTransferUrl; }
    public String getHanaApiKey() { return hanaApiKey; }
    public void setHanaApiKey(String hanaApiKey) { this.hanaApiKey = hanaApiKey; }
    public String getKbTransferUrl() { return kbTransferUrl; }
    public void setKbTransferUrl(String kbTransferUrl) { this.kbTransferUrl = kbTransferUrl; }
    public String getKbApiKey() { return kbApiKey; }
    public void setKbApiKey(String kbApiKey) { this.kbApiKey = kbApiKey; }
    public String getShinhanTransferUrl() { return shinhanTransferUrl; }
    public void setShinhanTransferUrl(String shinhanTransferUrl) { this.shinhanTransferUrl = shinhanTransferUrl; }
    public String getShinhanApiKey() { return shinhanApiKey; }
    public void setShinhanApiKey(String shinhanApiKey) { this.shinhanApiKey = shinhanApiKey; }
    public String getImTransferUrl() { return imTransferUrl; }
    public void setImTransferUrl(String imTransferUrl) { this.imTransferUrl = imTransferUrl; }
    public String getImApiKey() { return imApiKey; }
    public void setImApiKey(String imApiKey) { this.imApiKey = imApiKey; }
    public String getPensionSummaryUrl() { return pensionSummaryUrl; }
    public void setPensionSummaryUrl(String pensionSummaryUrl) { this.pensionSummaryUrl = pensionSummaryUrl; }
    public String getPensionApiKey() { return pensionApiKey; }
    public void setPensionApiKey(String pensionApiKey) { this.pensionApiKey = pensionApiKey; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
}
