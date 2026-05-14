package com.berty.adapter.out.notify;

import com.berty.application.port.out.NotificationChannelPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SmsNotificationAdapter implements NotificationChannelPort {
    private static final Logger log = LoggerFactory.getLogger(SmsNotificationAdapter.class);

    private final boolean stubMode;

    public SmsNotificationAdapter(@Value("${berty.notify.sms.stub-mode:true}") boolean stubMode) {
        this.stubMode = stubMode;
    }

    @Override
    public Channel channel() { return Channel.SMS; }

    @Override
    public boolean send(String userId, String title, String body) {
        if (stubMode) {
            log.info("[SMS stub] userId={} title={} body={}", userId, title, body);
            return true;
        }
        log.warn("[SMS] real provider not configured — drop userId={} title={}", userId, title);
        return false;
    }
}
