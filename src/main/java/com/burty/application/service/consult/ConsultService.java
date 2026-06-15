/**
 *
 *
 * <pre>
 * <b>Description  : 상담 애플리케이션 서비스 (ConsultService)</b>
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

import com.burty.application.port.in.action.ActionRecommendationUseCase;
import com.burty.application.port.in.cashflow.CashflowForecastUseCase;
import com.burty.application.port.in.cashflow.RiskAssessmentUseCase;
import com.burty.application.port.in.consult.ConsultUseCase;
import com.burty.application.port.in.user.PersonaInferenceUseCase;
import com.burty.application.port.out.ai.EasyReadPort;
import com.burty.application.port.out.ai.LlmPort;
import com.burty.application.port.out.mydata.MyDataPort;
import com.burty.domain.asset.model.AssetSnapshot;
import com.burty.domain.cashflow.model.RiskAssessment;
import com.burty.domain.consult.model.ConsultationResult;
import com.burty.domain.consult.model.MonthlyReport;
import com.burty.domain.user.entity.PersonaProfileEntity;
import com.burty.prompt.BurtyPrompts;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConsultService implements ConsultUseCase {

  private final EasyReadPort easyReadPort;
  private final MyDataPort myDataPort;
  private final LlmPort llmPort;
  private final CashflowForecastUseCase cashflowForecastUseCase;
  private final RiskAssessmentUseCase riskAssessmentUseCase;
  private final ActionRecommendationUseCase actionRecommendationUseCase;
  private final PersonaInferenceUseCase personaInferenceUseCase;

  @Override
  public ConsultationResult consult(String userId, String question) {
    AssetSnapshot snapshot = myDataPort.fetchAssetSnapshot(userId);
    PersonaProfileEntity persona = personaInferenceUseCase.getOrInfer(userId);
    RiskAssessment risk = riskAssessmentUseCase.assess(userId);

    String llmAnswer =
        llmPort.generate(
            BurtyPrompts.SYSTEM_FINANCIAL_ADVISOR,
            BurtyPrompts.advisoryUserPrompt(question, persona, snapshot, risk));
    return new ConsultationResult(
        easyReadPort.toEasyRead(llmAnswer),
        easyReadPort.toSignalColor(snapshot.volatilityPercent()),
        List.of("지출 점검", "정책 매칭 확인", "위험일 대비 자동이체 점검"));
  }

  @Override
  public MonthlyReport createMonthlyReport(String userId) {
    var forecast = cashflowForecastUseCase.forecast(userId);
    var risk = riskAssessmentUseCase.assess(userId);
    var action = actionRecommendationUseCase.topRecommendation(userId);
    String summary =
        easyReadPort.toEasyRead(
            "30일 현금흐름 점검 결과 최소 잔액은 %,d원입니다. 위험 단계는 %s 입니다. "
                    .formatted(forecast.minimumBalance(), risk.level())
                + "권장 행동은 "
                + action.title()
                + " 입니다.");
    return new MonthlyReport(
        userId,
        YearMonth.now().toString(),
        summary,
        risk.level(),
        action.title(),
        List.of(
            "예상 위험일: " + (risk.riskDate() == null ? "없음" : risk.riskDate()),
            "위험 근거: " + risk.reason(),
            "예상 개선효과: " + action.estimatedImprovement() + "원"));
  }
}
