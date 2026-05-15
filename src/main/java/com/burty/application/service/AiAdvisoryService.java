package com.burty.application.service;

import com.burty.application.port.in.AiAdvisoryUseCase;
import com.burty.application.port.in.PersonaInferenceUseCase;
import com.burty.application.port.in.RiskAssessmentUseCase;
import com.burty.application.port.out.EasyReadPort;
import com.burty.application.port.out.LlmPort;
import com.burty.application.port.out.MyDataPort;
import com.burty.domain.entity.PersonaProfileEntity;
import com.burty.domain.model.AssetSnapshot;
import com.burty.domain.model.ConsultationResult;
import com.burty.domain.model.RiskAssessment;
import com.burty.prompt.BurtyPrompts;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiAdvisoryService implements AiAdvisoryUseCase {
    private final MyDataPort myDataPort;
    private final LlmPort llmPort;
    private final EasyReadPort easyReadPort;
    private final PersonaInferenceUseCase personaInferenceUseCase;
    private final RiskAssessmentUseCase riskAssessmentUseCase;

    public AiAdvisoryService(MyDataPort myDataPort, LlmPort llmPort, EasyReadPort easyReadPort, PersonaInferenceUseCase personaInferenceUseCase, RiskAssessmentUseCase riskAssessmentUseCase) {
        this.myDataPort = myDataPort;
        this.llmPort = llmPort;
        this.easyReadPort = easyReadPort;
        this.personaInferenceUseCase = personaInferenceUseCase;
        this.riskAssessmentUseCase = riskAssessmentUseCase;
    }

    @Override
    public ConsultationResult consultWithAi(String userId, String question) {
        AssetSnapshot snapshot = myDataPort.fetchAssetSnapshot(userId);
        PersonaProfileEntity persona = personaInferenceUseCase.getOrInfer(userId);
        RiskAssessment risk = riskAssessmentUseCase.assess(userId);

        String llmAnswer = llmPort.generate(
                BurtyPrompts.SYSTEM_FINANCIAL_ADVISOR,
                BurtyPrompts.advisoryUserPrompt(question, persona, snapshot, risk)
        );
        String easySummary = easyReadPort.toEasyRead(llmAnswer);
        return new ConsultationResult(
                easySummary,
                easyReadPort.toSignalColor(snapshot.getVolatilityPercent()),
                List.of("지출 점검", "정책 매칭 확인", "위험일 대비 자동이체 점검")
        );
    }
}