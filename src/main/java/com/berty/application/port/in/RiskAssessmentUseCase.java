package com.berty.application.port.in;

import com.berty.domain.model.RiskAssessment;

public interface RiskAssessmentUseCase {
    RiskAssessment assess(String userId);
}
