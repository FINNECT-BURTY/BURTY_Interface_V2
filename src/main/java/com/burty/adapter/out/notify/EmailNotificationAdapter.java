package com.burty.adapter.out.notify;

import com.burty.application.port.out.notify.NotificationChannelPort;
import com.burty.application.service.notification.NotificationRecipientResolver;
import com.burty.config.NotifyProperties;
import com.burty.core.constant.LogMessages;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationAdapter implements NotificationChannelPort {

  private static final Logger log = LoggerFactory.getLogger(EmailNotificationAdapter.class);

  private final NotifyProperties notifyProperties;
  private final ObjectProvider<JavaMailSender> mailSender;
  private final NotificationRecipientResolver recipientResolver;

  public EmailNotificationAdapter(
      NotifyProperties notifyProperties,
      ObjectProvider<JavaMailSender> mailSender,
      NotificationRecipientResolver recipientResolver) {
    this.notifyProperties = notifyProperties;
    this.mailSender = mailSender;
    this.recipientResolver = recipientResolver;
  }

  @Override
  public Channel channel() {
    return Channel.EMAIL;
  }

  @Override
  public boolean send(String userId, String title, String body) {
    if (notifyProperties.getEmail().isStubMode()) {
      log.info(LogMessages.Notify.STUB_CHANNEL, "EMAIL", userId, title);
      return true;
    }
    JavaMailSender sender = mailSender.getIfAvailable();
    if (sender == null) {
      log.warn("[EMAIL] JavaMailSender not configured — drop userId={} title={}", userId, title);
      return false;
    }
    return recipientResolver
        .resolveEmail(userId)
        .map(
            email -> {
              try {
                MimeMessage message = sender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
                helper.setTo(email);
                helper.setSubject(title != null ? title : "BURTY 알림");
                helper.setText(body != null ? body : "", false);
                helper.setFrom(
                    notifyProperties.getEmail().getFromAddress(),
                    notifyProperties.getEmail().getFromName());
                sender.send(message);
                return true;
              } catch (Exception e) {
                log.error("[EMAIL] send failed userId={} error={}", userId, e.getMessage());
                return false;
              }
            })
        .orElseGet(
            () -> {
              log.warn("[EMAIL] no email for userId={} title={}", userId, title);
              return false;
            });
  }
}
