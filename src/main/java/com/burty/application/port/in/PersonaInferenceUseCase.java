package com.burty.application.port.in;

import com.burty.domain.entity.PersonaProfileEntity;

public interface PersonaInferenceUseCase {

    PersonaProfileEntity getOrInfer(String userId);

    PersonaProfileEntity overrideByUser(String userId, String occupationCode, String residenceCode, String householdType, Long monthlyIncomeAvg);

    PersonaProfileEntity reinfer(String userId);
}
