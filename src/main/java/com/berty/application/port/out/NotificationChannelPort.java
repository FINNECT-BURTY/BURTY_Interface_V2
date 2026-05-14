package com.berty.application.port.out;

public interface NotificationChannelPort {

    enum Channel { PUSH, SMS, EMAIL }

    Channel channel();

    /** Returns true if delivery succeeded (or queued for delivery). */
    boolean send(String userId, String title, String body);
}
