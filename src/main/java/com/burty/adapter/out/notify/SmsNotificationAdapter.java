package com.burty.adapter.out.notify;

import com.burty.application.port.out.notify.NotificationChannelPort;
import com.burty.application.service.notification.NotificationRecipientResolver;
import com.burty.config.NotifyProperties;
import com.burty.core.constant.LogMessages;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class SmsNotificationAdapter implements NotificationChannelPort {

  private static final Logger log = LoggerFactory.getLogger(SmsNotificationAdapter.class);

  private final NotifyProperties notifyProperties;
  private final RestTemplate restTemplate;
  private final NotificationRecipientResolver recipientResolver;

  public SmsNotificationAdapter(
      NotifyProperties notifyProperties,
      RestTemplate restTemplate,
      NotificationRecipientResolver recipientResolver) {
    this.notifyProperties = notifyProperties;
    this.restTemplate = restTemplate;
    this.recipientResolver = recipientResolver;
  }

  @Override
  public Channel channel() {
    return Channel.SMS;
  }

  @Override
  public boolean send(String userId, String title, String body) {
    if (notifyProperties.getSms().isStubMode()) {
      log.info(LogMessages.Notify.STUB_CHANNEL, "SMS", userId, title, body);
      return true;
    }
    if (!notifyProperties.getSms().isConfigured()) {
      log.warn("[SMS] provider not configured — drop userId={} title={}", userId, title);
      return false;
    }
    return recipientResolver
        .resolvePhone(userId)
        .map(
            phone -> {
              try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", buildAuthHeader());
                String text =
                    (title != null && !title.isBlank() ? title + "\n" : "")
                        + (body != null ? body : "");
                Map<String, Object> payload =
                    Map.of(
                        "message",
                        Map.of(
                            "to", phone,
                            "from", notifyProperties.getSms().getSenderNumber(),
                            "text", text));
                restTemplate.postForEntity(
                    notifyProperties.getSms().getApiUrl(),
                    new HttpEntity<>(payload, headers),
                    Map.class);
                return true;
              } catch (Exception e) {
                log.error("[SMS] send failed userId={} error={}", userId, e.getMessage());
                return false;
              }
            })
        .orElseGet(
            () -> {
              log.warn("[SMS] no phone for userId={} title={}", userId, title);
              return false;
            });
  }

  private String buildAuthHeader() {
    return SolapiAuthHeaderBuilder.build(
        notifyProperties.getSms().getApiKey(), notifyProperties.getSms().getApiSecret());
  }
}
