/**
 *
 *
 * <pre>
 * <b>Description  : 인증 API 컨트롤러 (AdminAuthController)</b>
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

import com.burty.application.dto.auth.AdminLoginRequest;
import com.burty.application.dto.auth.AdminRegisterRequest;
import com.burty.application.dto.auth.AdminRegisterResultResponse;
import com.burty.application.dto.auth.AdminTokenResponse;
import com.burty.application.port.in.auth.AdminAuthUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/auth")
@Tag(name = "Admin Auth", description = "관리자 인증 API (로그인 / 계정 등록)")
@RequiredArgsConstructor
public class AdminAuthController extends BaseController {

  private final AdminAuthUseCase adminAuthUseCase;

  @PostMapping("/login")
  @Operation(summary = "관리자 로그인", description = "아이디·비밀번호로 로그인해 ROLE_ADMIN JWT를 발급합니다.")
  public ApiResponse<AdminTokenResponse> login(@Valid @RequestBody AdminLoginRequest request) {
    String token = adminAuthUseCase.login(request.username(), request.password());
    return ApiResponse.ok(new AdminTokenResponse(token));
  }

  @PostMapping("/register")
  @Operation(
      summary = "관리자 계정 등록",
      description = "setup-key로 관리자 계정 생성 (role: ADMIN | SUPER_ADMIN).",
      security = {})
  public ApiResponse<AdminRegisterResultResponse> register(
      @Valid @RequestBody AdminRegisterRequest request) {
    adminAuthUseCase.register(
        request.setupKey(), request.username(), request.password(), request.role());
    return ApiResponse.ok(new AdminRegisterResultResponse(true));
  }
}
