/**
 *
 *
 * <pre>
 * <b>Description  : 상담 애플리케이션 서비스 (AiAdvisoryService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.consult
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
package com.burty.application.service.consult;

import com.burty.application.port.in.cashflow.RiskAssessmentUseCase;
import com.burty.application.port.in.consult.AiAdvisoryUseCase;
import com.burty.application.port.in.user.PersonaInferenceUseCase;
import com.burty.application.port.out.ai.EasyReadPort;
import com.burty.application.port.out.ai.LlmPort;
import com.burty.application.port.out.mydata.MyDataPort;
import com.burty.domain.asset.model.AssetSnapshot;
import com.burty.domain.cashflow.model.RiskAssessment;
import com.burty.domain.consult.model.ConsultationResult;
import com.burty.domain.user.entity.PersonaProfileEntity;
import com.burty.prompt.BurtyPrompts;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiAdvisoryService implements AiAdvisoryUseCase {
  private final MyDataPort myDataPort;
  private final LlmPort llmPort;
  private final EasyReadPort easyReadPort;
  private final PersonaInferenceUseCase personaInferenceUseCase;
  private final RiskAssessmentUseCase riskAssessmentUseCase;

  public AiAdvisoryService(
      MyDataPort myDataPort,
      LlmPort llmPort,
      EasyReadPort easyReadPort,
      PersonaInferenceUseCase personaInferenceUseCase,
      RiskAssessmentUseCase riskAssessmentUseCase) {
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

    String llmAnswer =
        llmPort.generate(
            BurtyPrompts.SYSTEM_FINANCIAL_ADVISOR,
            BurtyPrompts.advisoryUserPrompt(question, persona, snapshot, risk));
    String easySummary = easyReadPort.toEasyRead(llmAnswer);
    return new ConsultationResult(
        easySummary,
        easyReadPort.toSignalColor(snapshot.volatilityPercent()),
        List.of("지출 점검", "정책 매칭 확인", "위험일 대비 자동이체 점검"));
  }
}
