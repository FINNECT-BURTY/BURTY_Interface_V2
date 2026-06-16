/**
 *
 *
 * <pre>
 * <b>Description  : 인증 API 컨트롤러 (DemoController)</b>
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

import com.burty.application.dto.auth.DemoSessionResponse;
import com.burty.application.port.in.auth.DemoSessionUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/demo")
@Profile("!prod")
@Tag(name = "BURTY Demo", description = "MVP 시연용 사용자와 현금흐름 데이터를 생성합니다.")
@RequiredArgsConstructor
public class DemoController extends BaseController {

  private final DemoSessionUseCase demoSessionUseCase;

  @PostMapping("/session")
  @Operation(summary = "데모 세션 생성", description = "사회초년생 1인 가구 시나리오 데이터를 만들고 JWT를 반환합니다.")
  public ApiResponse<DemoSessionResponse> createDemoSession() {
    return ApiResponse.ok(demoSessionUseCase.createSession());
  }
}
