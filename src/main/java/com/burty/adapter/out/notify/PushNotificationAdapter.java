package com.burty.adapter.out.notify;

import com.burty.application.port.out.notify.NotificationChannelPort;
import com.burty.application.service.notification.NotificationRecipientResolver;
import com.burty.config.NotifyProperties;
import com.burty.core.constant.LogMessages;
import com.burty.util.PiiMasker;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PushNotificationAdapter implements NotificationChannelPort {

  private static final Logger log = LoggerFactory.getLogger(PushNotificationAdapter.class);

  private final NotifyProperties notifyProperties;
  private final RestTemplate restTemplate;
  private final NotificationRecipientResolver recipientResolver;
  private final FcmOAuthTokenProvider fcmOAuthTokenProvider;

  public PushNotificationAdapter(
      NotifyProperties notifyProperties,
      RestTemplate restTemplate,
      NotificationRecipientResolver recipientResolver,
      FcmOAuthTokenProvider fcmOAuthTokenProvider) {
    this.notifyProperties = notifyProperties;
    this.restTemplate = restTemplate;
    this.recipientResolver = recipientResolver;
    this.fcmOAuthTokenProvider = fcmOAuthTokenProvider;
  }

  @Override
  public Channel channel() {
    return Channel.PUSH;
  }

  @Override
  public boolean send(String userId, String title, String body) {
    if (notifyProperties.getPush().isStubMode()) {
      log.info(LogMessages.Notify.STUB_CHANNEL, "PUSH", userId, title);
      return true;
    }
    if (!notifyProperties.getPush().isConfigured()) {
      log.warn("[PUSH] FCM not configured — drop userId={} title={}", userId, title);
      return false;
    }
    List<String> tokens = recipientResolver.resolvePushTokens(userId);
    if (tokens.isEmpty()) {
      log.warn("[PUSH] no device token for userId={} title={}", userId, title);
      return false;
    }
    boolean anySuccess = false;
    for (String token : tokens) {
      if (sendFcm(token, title, body)) {
        anySuccess = true;
      }
    }
    return anySuccess;
  }

  @SuppressWarnings("unchecked")
  private boolean sendFcm(String token, String title, String body) {
    try {
      String url =
          "https://fcm.googleapis.com/v1/projects/"
              + notifyProperties.getPush().getFcmProjectId()
              + "/messages:send";
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.setBearerAuth(
          fcmOAuthTokenProvider.getAccessToken(notifyProperties.getPush().getFcmCredentialsJson()));
      Map<String, Object> payload =
          Map.of(
              "message",
              Map.of(
                  "token",
                  token,
                  "notification",
                  Map.of(
                      "title", title != null ? title : "BURTY",
                      "body", body != null ? body : "")));
      restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), Map.class);
      return true;
    } catch (Exception e) {
      log.error("[PUSH] FCM 발송 실패 token={} error={}", PiiMasker.secret(token), e.getMessage());
      return false;
    }
  }
}
