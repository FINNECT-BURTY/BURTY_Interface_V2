package com.burty.adapter.in.web;

import com.burty.adapter.in.web.dto.SimpleResultResponse;
import com.burty.adapter.in.web.dto.UserFeedbackRequest;
import com.burty.core.dto.response.ApiResponse;
import com.burty.domain.entity.ActionFeedbackEntity;
import com.burty.domain.repository.ActionFeedbackRepository;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/burty/feedback")
@Tag(name = "BURTY Feedback", description = "추천 도움 여부/실행 여부/금액 정확도/고정비 여부 피드백 API")
public class UserFeedbackController {
    private final ActionFeedbackRepository actionFeedbackRepository;

    public UserFeedbackController(ActionFeedbackRepository actionFeedbackRepository) {
        this.actionFeedbackRepository = actionFeedbackRepository;
    }

    @PostMapping
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "사용자 피드백 저장", description = "추천 도움 여부, 실행 여부, 금액 정확도, 고정비 여부 등 일반 피드백을 저장합니다.")
    public ApiResponse<SimpleResultResponse> submit(@RequestBody UserFeedbackRequest request) {
        ActionFeedbackEntity entity = new ActionFeedbackEntity();
        entity.setUserId(request.getUserId());
        entity.setActionType(defaultString(request.getTargetType(), "GENERAL") + ":" + defaultString(request.getTargetId(), "-"));
        entity.setFeedback(defaultString(request.getFeedbackType(), "feedback") + "=" + defaultString(request.getFeedbackValue(), "-"));
        entity.setCreatedAt(LocalDateTime.now());
        actionFeedbackRepository.save(entity);
        return ApiResponse.ok(new SimpleResultResponse(true, "피드백이 저장되었습니다."));
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
