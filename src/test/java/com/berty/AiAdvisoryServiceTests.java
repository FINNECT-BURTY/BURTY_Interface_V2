package com.berty;

import com.berty.application.port.in.PersonaInferenceUseCase;
import com.berty.application.port.in.RiskAssessmentUseCase;
import com.berty.application.port.out.EasyReadPort;
import com.berty.application.port.out.LlmPort;
import com.berty.application.port.out.MyDataPort;
import com.berty.application.service.AiAdvisoryService;
import com.berty.domain.entity.PersonaProfileEntity;
import com.berty.domain.model.AssetSnapshot;
import com.berty.domain.model.ConsultationResult;
import com.berty.domain.model.RiskAssessment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AiAdvisoryServiceTests {

    @Test
    void consultWithAi_returnsEasyReadResultWithSignal() {
        MyDataPort myDataPort = userId -> new AssetSnapshot(100_000_000, 2_000_000, 12.5);
        LlmPort llmPort = (systemPrompt, userPrompt) ->
                "포트폴리오 상태가 보통이에요. 이번 달 지출이 커졌어요. 자동이체 점검하세요.";
        EasyReadPort easyReadPort = new EasyReadPort() {
            @Override
            public String toEasyRead(String rawText) {
                return rawText;
            }

            @Override
            public String toSignalColor(double volatilityPercent) {
                return "YELLOW";
            }
        };
        PersonaInferenceUseCase personaUseCase = new PersonaInferenceUseCase() {
            @Override
            public PersonaProfileEntity getOrInfer(String userId) {
                PersonaProfileEntity p = new PersonaProfileEntity();
                p.setOccupationCode("NEW_WORKER");
                p.setResidenceCode("MONTHLY_RENT");
                p.setHouseholdType("SINGLE");
                p.setMonthlyIncomeAvg(2_500_000L);
                p.setIncomeVariabilityPct(12.5);
                p.setAge(28);
                return p;
            }
            @Override
            public PersonaProfileEntity overrideByUser(String userId, String occupationCode, String residenceCode, String householdType, Long monthlyIncomeAvg) {
                return getOrInfer(userId);
            }
            @Override
            public PersonaProfileEntity reinfer(String userId) {
                return getOrInfer(userId);
            }
        };
        RiskAssessmentUseCase riskUseCase = userId -> new RiskAssessment(userId, "GREEN", 50_000L, "안정", null, 1_500_000L);
        AiAdvisoryService service = new AiAdvisoryService(myDataPort, llmPort, easyReadPort, personaUseCase, riskUseCase);

        ConsultationResult result = service.consultWithAi("u1", "이번달 괜찮아?");

        Assertions.assertEquals("YELLOW", result.getSignalColor());
        Assertions.assertTrue(result.getSummary().contains("자동이체"));
        Assertions.assertFalse(result.getRecommendedActions().isEmpty());
    }
}
