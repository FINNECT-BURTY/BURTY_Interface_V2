package com.burty.adapter.in.web;

import com.burty.adapter.in.web.dto.EmailLoginRequest;
import com.burty.adapter.in.web.dto.EmailRegisterRequest;
import com.burty.adapter.in.web.dto.SocialLoginResponse;
import com.burty.application.port.in.EmailAuthUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.domain.model.SocialLoginResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/email")
@Tag(name = "Email Auth", description = "이메일 회원가입 / 로그인")
public class EmailAuthController extends BaseController {

    private final EmailAuthUseCase emailAuthUseCase;

    public EmailAuthController(EmailAuthUseCase emailAuthUseCase) {
        this.emailAuthUseCase = emailAuthUseCase;
    }

    @PostMapping("/register")
    @Operation(
            summary = "이메일 회원가입",
            description = "이메일과 비밀번호로 회원가입합니다. 가입 즉시 토큰을 발급합니다.\n\n" +
                    "**비밀번호 조건:** 영문자 + 숫자 포함 8자 이상"
    )
    public ResponseEntity<ApiResponse<SocialLoginResponse>> register(
            @RequestBody EmailRegisterRequest request
    ) {
        SocialLoginResult result = emailAuthUseCase.register(request.getEmail(), request.getPassword());
        return created(toResponse(result), "회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    @Operation(
            summary = "이메일 로그인",
            description = "이메일과 비밀번호로 로그인합니다."
    )
    public ResponseEntity<ApiResponse<SocialLoginResponse>> login(
            @RequestBody EmailLoginRequest request
    ) {
        SocialLoginResult result = emailAuthUseCase.login(request.getEmail(), request.getPassword());
        return success(toResponse(result));
    }

    private SocialLoginResponse toResponse(SocialLoginResult result) {
        return new SocialLoginResponse(
                result.getUserId(),
                result.getProvider(),
                result.getAccessToken(),
                result.getRefreshToken(),
                result.getAccessExpiresInSeconds(),
                result.getRefreshExpiresInSeconds(),
                result.isNewUser(),
                result.isProfileComplete()
        );
    }
}
