/**
 *
 *
 * <pre>
 * <b>Description  : 행동추천 API 컨트롤러 (ActionTrackingController)</b>
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

import com.burty.application.dto.action.ActionTrackingResponse;
import com.burty.application.port.in.action.ActionTrackingUseCase;
import com.burty.core.annotation.CurrentUserId;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/actions/tracking")
@RequiredArgsConstructor
@Tag(name = "BURTY Action Tracking", description = "추천 행동 수락/실행/효과 추적 API")
public class ActionTrackingController extends BaseController {

  private final ActionTrackingUseCase actionTrackingUseCase;

  @GetMapping("/{actionType}")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "추천 행동 효과 추적", description = "수락/거절/실행 횟수와 현재 위험도를 함께 반환합니다.")
  public ApiResponse<ActionTrackingResponse> tracking(
      @CurrentUserId String userId, @PathVariable String actionType) {
    return ApiResponse.ok(actionTrackingUseCase.tracking(userId, actionType));
  }
}
