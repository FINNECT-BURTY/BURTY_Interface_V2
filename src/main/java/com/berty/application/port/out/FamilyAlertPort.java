package com.berty.application.port.out;

import com.berty.domain.model.FamilyAlert;

import java.util.List;

public interface FamilyAlertPort {
    void send(String userId, String message);
    List<FamilyAlert> findByUserId(String userId);
}
