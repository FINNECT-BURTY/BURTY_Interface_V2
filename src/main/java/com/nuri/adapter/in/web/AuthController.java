package com.nuri.adapter.in.web;

import com.nuri.adapter.in.web.dto.TokenIssueRequest;
import com.nuri.core.dto.response.ApiResponse;
import com.nuri.security.JwtBlacklistService;
import com.nuri.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/nuri/auth")
@Tag(name = "NURI Auth", description = "NURI 인증(JWT 발급/로그아웃) API")
public class AuthController {
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtBlacklistService jwtBlacklistService;

    public AuthController(JwtTokenProvider jwtTokenProvider, JwtBlacklistService jwtBlacklistService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtBlacklistService = jwtBlacklistService;
    }

    @PostMapping("/token")
    @Operation(
            summary = "JWT 발급",
            description = "userId 기반으로 접근 토큰을 발급합니다.",
            security = {}
    )
    public ApiResponse<Map<String, String>> issueToken(@RequestBody TokenIssueRequest request) {
        String userId = request.getUserId() != null ? request.getUserId() : UUID.randomUUID().toString();
        String token = jwtTokenProvider.generateToken(userId);
        return ApiResponse.ok(Map.of("accessToken", token));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "로그아웃",
            description = "현재 Bearer 토큰을 블랙리스트에 등록합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    public ApiResponse<Map<String, Object>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        jwtBlacklistService.blacklist(token);
        return ApiResponse.ok(Map.of("logout", true));
    }
}
