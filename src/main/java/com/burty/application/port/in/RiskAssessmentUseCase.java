package com.burty.application.port.in;

import com.burty.domain.model.RiskAssessment;

public interface RiskAssessmentUseCase {
    RiskAssessment assess(String userId);
}
