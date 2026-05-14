package com.berty.application.port.in;

import com.berty.domain.model.ActionRecommendation;

public interface ActionRecommendationUseCase {
    ActionRecommendation topRecommendation(String userId);
}
