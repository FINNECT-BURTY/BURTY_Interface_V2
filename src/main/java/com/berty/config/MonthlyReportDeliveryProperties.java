package com.berty.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "berty.report.delivery")
public class MonthlyReportDeliveryProperties {
    /**
     * 월간 리포트 메타데이터(또는 PDF)를 전달할 운영 웹훅 URL.
     */
    private String webhookUrl = "";
    private boolean webhookEnabled = false;
    /**
     * true면 웹훅 JSON에 PDF 전체를 Base64로 포함합니다(대용량·민감정보 주의).
     */
    private boolean attachPdfBase64 = false;

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public boolean isWebhookEnabled() {
        return webhookEnabled;
    }

    public void setWebhookEnabled(boolean webhookEnabled) {
        this.webhookEnabled = webhookEnabled;
    }

    public boolean isAttachPdfBase64() {
        return attachPdfBase64;
    }

    public void setAttachPdfBase64(boolean attachPdfBase64) {
        this.attachPdfBase64 = attachPdfBase64;
    }
}
