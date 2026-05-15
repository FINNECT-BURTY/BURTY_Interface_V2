package com.burty.application.port.in;

import com.burty.domain.model.ActionRecommendation;

public interface ActionRecommendationUseCase {
    ActionRecommendation topRecommendation(String userId);
}
