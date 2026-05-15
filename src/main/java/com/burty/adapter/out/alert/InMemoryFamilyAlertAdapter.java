package com.burty.adapter.out.alert;

import com.burty.application.port.out.FamilyAlertPort;
import com.burty.domain.model.FamilyAlert;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
        return store.stream().filter(it -> it.getUserId().equals(userId)).toList();
    }
}
