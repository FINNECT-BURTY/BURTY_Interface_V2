package com.nuri.application.port.in;

import com.nuri.domain.model.ConsultationResult;

public interface AiAdvisoryUseCase {
    ConsultationResult consultWithAi(String userId, String question);
}
