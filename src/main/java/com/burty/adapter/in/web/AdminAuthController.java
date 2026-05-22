package com.burty.adapter.in.web;

import com.burty.adapter.in.web.dto.AdminLoginRequest;
import com.burty.adapter.in.web.dto.AdminRegisterRequest;
import com.burty.adapter.in.web.dto.AdminTokenResponse;
import com.burty.application.port.in.AdminAuthUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/auth")
@Tag(name = "Admin Auth", description = "관리자 인증 API (로그인 / 계정 등록)")
public class AdminAuthController extends BaseController {

    private final AdminAuthUseCase adminAuthUseCase;

    public AdminAuthController(AdminAuthUseCase adminAuthUseCase) {
        this.adminAuthUseCase = adminAuthUseCase;
    }

    @PostMapping("/login")
    @Operation(
            summary = "관리자 로그인",
            description = "아이디/비밀번호로 로그인하여 ROLE_ADMIN JWT를 발급합니다.",
            security = {}
    )
    public ApiResponse<AdminTokenResponse> login(@RequestBody AdminLoginRequest request) {
        String token = adminAuthUseCase.login(request.getUsername(), request.getPassword());
        return ApiResponse.ok(new AdminTokenResponse(token));
    }

    @PostMapping("/register")
    @Operation(
            summary = "관리자 계정 등록",
            description = "셋업 키(burty.admin.setup-key)를 알고 있는 경우 관리자 계정을 생성합니다. role: ADMIN | SUPER_ADMIN",
            security = {}
    )
    public ApiResponse<Map<String, Boolean>> register(@RequestBody AdminRegisterRequest request) {
        adminAuthUseCase.register(
                request.getSetupKey(),
                request.getUsername(),
                request.getPassword(),
                request.getRole()
        );
        return ApiResponse.ok(Map.of("registered", true));
    }
}
