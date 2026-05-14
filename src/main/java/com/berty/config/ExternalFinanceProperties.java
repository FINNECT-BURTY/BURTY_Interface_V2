package com.berty.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "berty.external")
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
    private String openBankingAccountsUrl = "https://testapi.openbanking.or.kr/v2.0/account/list";
    private String openBankingBalanceUrl = "https://testapi.openbanking.or.kr/v2.0/account/balance/fin_num";
    private String openBankingTransactionsUrl = "https://testapi.openbanking.or.kr/v2.0/account/transaction_list/fin_num";
    private String openBankingTransferUrl = "https://testapi.openbanking.or.kr/v2.0/transfer/withdraw/fin_num";
    private String openBankingClientId = "openbanking-client-id";
    private String openBankingClientSecret = "openbanking-client-secret";
    private String openBankingAccessToken = "openbanking-access-token";
    private String openBankingRefreshToken = "openbanking-refresh-token";
    private String openBankingTokenUrl = "https://testapi.openbanking.or.kr/oauth/2.0/token";
    private int openBankingRetryCount = 1;
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
    public String getOpenBankingAccountsUrl() { return openBankingAccountsUrl; }
    public void setOpenBankingAccountsUrl(String openBankingAccountsUrl) { this.openBankingAccountsUrl = openBankingAccountsUrl; }
    public String getOpenBankingBalanceUrl() { return openBankingBalanceUrl; }
    public void setOpenBankingBalanceUrl(String openBankingBalanceUrl) { this.openBankingBalanceUrl = openBankingBalanceUrl; }
    public String getOpenBankingTransactionsUrl() { return openBankingTransactionsUrl; }
    public void setOpenBankingTransactionsUrl(String openBankingTransactionsUrl) { this.openBankingTransactionsUrl = openBankingTransactionsUrl; }
    public String getOpenBankingTransferUrl() { return openBankingTransferUrl; }
    public void setOpenBankingTransferUrl(String openBankingTransferUrl) { this.openBankingTransferUrl = openBankingTransferUrl; }
    public String getOpenBankingClientId() { return openBankingClientId; }
    public void setOpenBankingClientId(String openBankingClientId) { this.openBankingClientId = openBankingClientId; }
    public String getOpenBankingClientSecret() { return openBankingClientSecret; }
    public void setOpenBankingClientSecret(String openBankingClientSecret) { this.openBankingClientSecret = openBankingClientSecret; }
    public String getOpenBankingAccessToken() { return openBankingAccessToken; }
    public void setOpenBankingAccessToken(String openBankingAccessToken) { this.openBankingAccessToken = openBankingAccessToken; }
    public String getOpenBankingRefreshToken() { return openBankingRefreshToken; }
    public void setOpenBankingRefreshToken(String openBankingRefreshToken) { this.openBankingRefreshToken = openBankingRefreshToken; }
    public String getOpenBankingTokenUrl() { return openBankingTokenUrl; }
    public void setOpenBankingTokenUrl(String openBankingTokenUrl) { this.openBankingTokenUrl = openBankingTokenUrl; }
    public int getOpenBankingRetryCount() { return openBankingRetryCount; }
    public void setOpenBankingRetryCount(int openBankingRetryCount) { this.openBankingRetryCount = openBankingRetryCount; }
    public String getPensionSummaryUrl() { return pensionSummaryUrl; }
    public void setPensionSummaryUrl(String pensionSummaryUrl) { this.pensionSummaryUrl = pensionSummaryUrl; }
    public String getPensionApiKey() { return pensionApiKey; }
    public void setPensionApiKey(String pensionApiKey) { this.pensionApiKey = pensionApiKey; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
}
