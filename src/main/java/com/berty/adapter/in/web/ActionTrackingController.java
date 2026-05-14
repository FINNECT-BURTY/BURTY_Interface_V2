package com.berty.adapter.in.web;

import com.berty.adapter.in.web.dto.ActionTrackingResponse;
import com.berty.application.port.in.RiskAssessmentUseCase;
import com.berty.core.dto.response.ApiResponse;
import com.berty.domain.model.RiskAssessment;
import com.berty.domain.repository.ActionExecutionRepository;
import com.berty.domain.repository.ActionFeedbackRepository;
import com.berty.security.AuthLevel;
import com.berty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/berty/actions/tracking")
@Tag(name = "BERTY Action Tracking", description = "추천 행동 수락/실행/효과 추적 API")
public class ActionTrackingController {
    private final ActionExecutionRepository actionExecutionRepository;
    private final ActionFeedbackRepository actionFeedbackRepository;
    private final RiskAssessmentUseCase riskAssessmentUseCase;

    public ActionTrackingController(ActionExecutionRepository actionExecutionRepository,
                                    ActionFeedbackRepository actionFeedbackRepository,
                                    RiskAssessmentUseCase riskAssessmentUseCase) {
        this.actionExecutionRepository = actionExecutionRepository;
        this.actionFeedbackRepository = actionFeedbackRepository;
        this.riskAssessmentUseCase = riskAssessmentUseCase;
    }

    @GetMapping("/{actionType}")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "추천 행동 효과 추적", description = "수락/거절/실행 횟수와 현재 위험도를 함께 반환합니다.")
    public ApiResponse<ActionTrackingResponse> tracking(@RequestParam String userId, @PathVariable String actionType) {
        RiskAssessment risk = riskAssessmentUseCase.assess(userId);
        return ApiResponse.ok(new ActionTrackingResponse(
                userId,
                actionType,
                actionExecutionRepository.countByUserIdAndActionType(userId, actionType),
                actionFeedbackRepository.countByUserIdAndActionTypeAndFeedbackIgnoreCase(userId, actionType, "accept"),
                actionFeedbackRepository.countByUserIdAndActionTypeAndFeedbackIgnoreCase(userId, actionType, "reject"),
                risk.getProjectedBalance(),
                risk.getLevel()
        ));
    }
}
