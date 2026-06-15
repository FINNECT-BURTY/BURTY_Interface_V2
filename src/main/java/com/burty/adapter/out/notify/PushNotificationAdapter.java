/**
 *
 *
 * <pre>
 * <b>Description  : 외부연동 외부 연동 어댑터 (PushNotificationAdapter)</b>
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PushNotificationAdapter extends AbstractStubNotificationChannelAdapter {

  public PushNotificationAdapter(@Value("${burty.notify.push.stub-mode:true}") boolean stubMode) {
    super(stubMode, "PUSH");
  }

  @Override
  public Channel channel() {
    return Channel.PUSH;
  }
}
