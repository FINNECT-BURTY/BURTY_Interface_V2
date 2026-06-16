/**
 *
 *
 * <pre>
 * <b>Description  : 외부연동 외부 연동 어댑터 (InMemoryFamilyAlertAdapter)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.out.alert
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
package com.burty.adapter.out.alert;

import com.burty.application.port.out.notify.FamilyAlertPort;
import com.burty.domain.family.model.FamilyAlert;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(FamilyAlertPort.class)
public class InMemoryFamilyAlertAdapter implements FamilyAlertPort {
  private final CopyOnWriteArrayList<FamilyAlert> store = new CopyOnWriteArrayList<>();
  private final FamilyAlertSseBroker sseBroker;

  public InMemoryFamilyAlertAdapter(FamilyAlertSseBroker sseBroker) {
    this.sseBroker = sseBroker;
  }

  @Override
  public void send(String userId, String message) {
    FamilyAlert alert = new FamilyAlert(userId, message, LocalDateTime.now());
    store.add(alert);
    sseBroker.publish(alert);
  }

  @Override
  public List<FamilyAlert> findByUserId(String userId) {
    return store.stream().filter(it -> it.userId().equals(userId)).toList();
  }
}
