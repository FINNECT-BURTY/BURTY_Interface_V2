/**
 *
 *
 * <pre>
 * <b>Description  : 외부연동 외부 연동 어댑터 (AbstractStubNotificationChannelAdapter)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.out.notify
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
package com.burty.adapter.out.notify;

import com.burty.application.port.out.notify.NotificationChannelPort;
import com.burty.core.constant.LogMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractStubNotificationChannelAdapter implements NotificationChannelPort {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final boolean stubMode;
  private final String channelLabel;

  protected AbstractStubNotificationChannelAdapter(boolean stubMode, String channelLabel) {
    this.stubMode = stubMode;
    this.channelLabel = channelLabel;
  }

  @Override
  public boolean send(String userId, String title, String body) {
    if (stubMode) {
      log.info(LogMessages.Notify.STUB_CHANNEL, channelLabel, userId, title, body);
      return true;
    }
    log.warn(
        "[{}] real provider not configured — drop userId={} title={}", channelLabel, userId, title);
    return false;
  }
}
