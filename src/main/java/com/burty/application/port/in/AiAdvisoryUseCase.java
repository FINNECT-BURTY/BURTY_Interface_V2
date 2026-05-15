package com.burty.application.port.in;

import com.burty.domain.model.ConsultationResult;

public interface AiAdvisoryUseCase {
    ConsultationResult consultWithAi(String userId, String question);
}
