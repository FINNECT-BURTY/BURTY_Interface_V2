package com.burty.adapter.out.notify;

import com.burty.application.port.out.NotificationChannelPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PushNotificationAdapter implements NotificationChannelPort {
    private static final Logger log = LoggerFactory.getLogger(PushNotificationAdapter.class);

    private final boolean stubMode;

    public PushNotificationAdapter(@Value("${burty.notify.push.stub-mode:true}") boolean stubMode) {
        this.stubMode = stubMode;
    }

    @Override
    public Channel channel() { return Channel.PUSH; }

    @Override
    public boolean send(String userId, String title, String body) {
        if (stubMode) {
            log.info("[PUSH stub] userId={} title={} body={}", userId, title, body);
            return true;
        }
        log.warn("[PUSH] real provider not configured — drop userId={} title={}", userId, title);
        return false;
    }
}
