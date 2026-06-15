/**
 *
 *
 * <pre>
 * <b>Description  : [테스트] 공통 통합 테스트 (AiAdvisoryServiceTests)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty;

import com.burty.application.port.in.cashflow.RiskAssessmentUseCase;
import com.burty.application.port.in.user.PersonaInferenceUseCase;
import com.burty.application.port.out.ai.EasyReadPort;
import com.burty.application.port.out.ai.LlmPort;
import com.burty.application.port.out.mydata.MyDataPort;
import com.burty.application.service.consult.AiAdvisoryService;
import com.burty.domain.asset.model.AssetSnapshot;
import com.burty.domain.cashflow.model.RiskAssessment;
import com.burty.domain.consult.model.ConsultationResult;
import com.burty.domain.user.entity.PersonaProfileEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AiAdvisoryServiceTests {

  @Test
  void consultWithAi_returnsEasyReadResultWithSignal() {
    MyDataPort myDataPort = userId -> new AssetSnapshot(100_000_000, 2_000_000, 12.5);
    LlmPort llmPort = (systemPrompt, userPrompt) -> "포트폴리오 상태가 보통이에요. 이번 달 지출이 커졌어요. 자동이체 점검하세요.";
    EasyReadPort easyReadPort =
        new EasyReadPort() {
          @Override
          public String toEasyRead(String rawText) {
            return rawText;
          }

          @Override
          public String toSignalColor(double volatilityPercent) {
            return "YELLOW";
          }
        };
    PersonaInferenceUseCase personaUseCase =
        new PersonaInferenceUseCase() {
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
          public PersonaProfileEntity overrideByUser(
              String userId,
              String occupationCode,
              String residenceCode,
              String householdType,
              Long monthlyIncomeAvg) {
            return getOrInfer(userId);
          }

          @Override
          public PersonaProfileEntity reinfer(String userId) {
            return getOrInfer(userId);
          }
        };
    RiskAssessmentUseCase riskUseCase =
        userId -> new RiskAssessment(userId, "GREEN", 50_000L, "안정", null, 1_500_000L);
    AiAdvisoryService service =
        new AiAdvisoryService(myDataPort, llmPort, easyReadPort, personaUseCase, riskUseCase);

    ConsultationResult result = service.consultWithAi("u1", "이번달 괜찮아?");

    Assertions.assertEquals("YELLOW", result.signalColor());
    Assertions.assertTrue(result.summary().contains("자동이체"));
    Assertions.assertFalse(result.recommendedActions().isEmpty());
  }
}
