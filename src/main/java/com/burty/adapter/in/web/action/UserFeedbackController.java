/**
 *
 *
 * <pre>
 * <b>Description  : 행동추천 API 컨트롤러 (UserFeedbackController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.action
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
package com.burty.adapter.in.web.action;

import com.burty.application.dto.shared.SimpleResultResponse;
import com.burty.application.dto.user.UserFeedbackRequest;
import com.burty.application.port.in.action.UserFeedbackUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
@Tag(name = "BURTY Feedback", description = "추천 도움 여부/실행 여부/금액 정확도/고정비 여부 피드백 API")
public class UserFeedbackController extends BaseController {

  private final UserFeedbackUseCase userFeedbackUseCase;

  @PostMapping
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(
      summary = "사용자 피드백 저장",
      description = "추천 도움 여부, 실행 여부, 금액 정확도, 고정비 여부 등 일반 피드백을 저장합니다.")
  public ApiResponse<SimpleResultResponse> submit(@RequestBody UserFeedbackRequest request) {
    userFeedbackUseCase.submit(request);
    return ApiResponse.ok(new SimpleResultResponse(true, "피드백이 저장되었습니다."));
  }
}
