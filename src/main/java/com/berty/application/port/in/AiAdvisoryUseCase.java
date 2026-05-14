package com.berty.application.port.in;

import com.berty.domain.model.ConsultationResult;

public interface AiAdvisoryUseCase {
    ConsultationResult consultWithAi(String userId, String question);
}
