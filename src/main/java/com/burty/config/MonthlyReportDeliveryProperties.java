/**
 *
 *
 * <pre>
 * <b>Description  : 설정 설정 프로퍼티 (MonthlyReportDeliveryProperties)</b>
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
@ConfigurationProperties(prefix = "burty.report.delivery")
public class MonthlyReportDeliveryProperties {
  /** 월간 리포트 메타데이터(또는 PDF)를 전달할 운영 웹훅 URL. */
  private String webhookUrl = "";

  private boolean webhookEnabled = false;

  /** true면 웹훅 JSON에 PDF 전체를 Base64로 포함합니다(대용량·민감정보 주의). */
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
