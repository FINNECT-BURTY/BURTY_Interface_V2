/**
 *
 *
 * <pre>
 * <b>Description  : 인증 API 컨트롤러 (LoginRiskController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.auth
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
package com.burty.adapter.in.web.auth;

import com.burty.application.dto.auth.LoginRiskEvaluateRequest;
import com.burty.application.dto.auth.LoginRiskEvaluateResponse;
import com.burty.application.port.in.auth.LoginRiskUseCase;
import com.burty.core.annotation.CurrentUserId;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/security/login-risk")
@RequiredArgsConstructor
@Tag(name = "BURTY Login Risk", description = "새 기기/IP/시간대 기반 이상 로그인 평가 API")
public class LoginRiskController extends BaseController {

  private final LoginRiskUseCase loginRiskUseCase;

  @PostMapping("/evaluate")
  @AuthLevel(RiskLevel.LEVEL_2)
  public ApiResponse<LoginRiskEvaluateResponse> evaluate(
      @CurrentUserId String userId, @Valid @RequestBody LoginRiskEvaluateRequest request) {
    return ApiResponse.ok(loginRiskUseCase.evaluate(userId, request));
  }
}
