package com.burty.application.service;

import com.burty.application.port.in.ActionRecommendationUseCase;
import com.burty.application.port.in.CashflowForecastUseCase;
import com.burty.application.port.in.PersonaInferenceUseCase;
import com.burty.application.port.out.EasyReadPort;
import com.burty.domain.entity.ActionFeedbackScoreEntity;
import com.burty.domain.entity.ActionRecommendationEntity;
import com.burty.domain.entity.PersonaProfileEntity;
import com.burty.domain.model.ActionRecommendation;
import com.burty.domain.model.CashflowForecast;
import com.burty.domain.repository.ActionFeedbackScoreRepository;
import com.burty.domain.repository.ActionRecommendationRepository;
import com.burty.util.BanditScorer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ActionRecommendationService implements ActionRecommendationUseCase {
    private static final Logger log = LoggerFactory.getLogger(ActionRecommendationService.class);

    private final CashflowForecastUseCase forecastUseCase;
    private final ActionRecommendationRepository recommendationRepository;
    private final ActionFeedbackScoreRepository feedbackScoreRepository;
    private final PersonaInferenceUseCase personaInferenceUseCase;
    private final EasyReadPort easyReadPort;
    private final BanditScorer banditScorer;

    public ActionRecommendationService(CashflowForecastUseCase forecastUseCase, ActionRecommendationRepository recommendationRepository, ActionFeedbackScoreRepository feedbackScoreRepository, PersonaInferenceUseCase personaInferenceUseCase, EasyReadPort easyReadPort, BanditScorer banditScorer) {
        this.forecastUseCase = forecastUseCase;
        this.recommendationRepository = recommendationRepository;
        this.feedbackScoreRepository = feedbackScoreRepository;
        this.personaInferenceUseCase = personaInferenceUseCase;
        this.easyReadPort = easyReadPort;
        this.banditScorer = banditScorer;
    }

    @Override
    public ActionRecommendation topRecommendation(String userId) {
        CashflowForecast forecast = forecastUseCase.forecast(userId);
        long minBalance = forecast.getMinimumBalance();
        PersonaProfileEntity persona = personaInferenceUseCase.getOrInfer(userId);
        String occupationCode = persona.getOccupationCode();

        List<ActionRecommendationEntity> all = recommendationRepository.findByActiveTrue();
        List<ActionRecommendation> candidates = new ArrayList<>();
        for (ActionRecommendationEntity rec : all) {
            if (!matchesBalance(rec, minBalance)) continue;
            if (!matchesOccupation(rec, occupationCode)) continue;
            double score = rec.getBaseScore() + feedbackBoost(userId, rec.getActionTypeCode());
            if (rec.getOccupationCode() != null && rec.getOccupationCode().equalsIgnoreCase(occupationCode)) {
                score += 10.0;
            }
            candidates.add(new ActionRecommendation(
                    rec.getActionTypeCode(),
                    rec.getTitleTemplate(),
                    easyReadPort.toEasyRead(rec.getDescriptionTemplate()),
                    rec.getEstimatedImprovement(),
                    score
            ));
        }

        ActionRecommendation selected = banditScorer.chooseTop(candidates, ActionRecommendation::getPriorityScore);
        if (selected == null) {
            selected = new ActionRecommendation(
                    "NO_ACTION", "현 상태 유지",
                    easyReadPort.toEasyRead("현재 30일 위험 구간이 낮아 유지 전략을 권장합니다."),
                    0, 10);
        }
        log.info("KPI action userId={} action={} score={} occupation={} candidates={} epsilon={}",
                userId, selected.getActionType(), selected.getPriorityScore(),
                occupationCode, candidates.size(), banditScorer.explorationEpsilon());
        return selected;
    }

    private boolean matchesBalance(ActionRecommendationEntity rec, long minBalance) {
        if (rec.getMinMinBalance() != null && minBalance < rec.getMinMinBalance()) return false;
        if (rec.getMaxMinBalance() != null && minBalance > rec.getMaxMinBalance()) return false;
        return true;
    }

    private boolean matchesOccupation(ActionRecommendationEntity rec, String occupationCode) {
        if (rec.getOccupationCode() == null) return true;
        return rec.getOccupationCode().equalsIgnoreCase(occupationCode);
    }

    private int feedbackBoost(String userId, String actionTypeCode) {
        return feedbackScoreRepository.findByUserIdAndActionTypeCode(userId, actionTypeCode)
                .map(ActionFeedbackScoreEntity::getScore)
                .map(s -> Math.max(-20, Math.min(20, s * 2)))
                .orElse(0);
    }
}
