package com.burty.adapter.in.web;

import com.burty.core.controller.BaseController;

import com.burty.application.port.in.RegisteredAccountUseCase;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/registered-accounts")
@Tag(name = "BURTY Registered Accounts", description = "이체 등록 계좌 관리")
public class RegisteredAccountController extends BaseController {

    private final RegisteredAccountUseCase useCase;

    public RegisteredAccountController(RegisteredAccountUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @AuthLevel(RiskLevel.LEVEL_2)
    @Operation(summary = "이체 등록 계좌 추가", description = "계좌번호를 입력한 그대로 저장합니다.")
    public ApiResponse<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        var saved = useCase.register(request.userId(), request.accountNo(), request.alias());
        return ApiResponse.ok(Map.of(
                "registered", true,
                "accountNo", saved.getAccountNo()
        ));
    }

    @GetMapping
    @AuthLevel(RiskLevel.LEVEL_2)
    @Operation(summary = "등록 계좌 목록")
    public ApiResponse<List<RegisteredAccountUseCase.View>> list(@RequestParam String userId) {
        return ApiResponse.ok(useCase.list(userId));
    }

    @DeleteMapping
    @AuthLevel(RiskLevel.LEVEL_2)
    @Operation(summary = "등록 계좌 해제")
    public ApiResponse<Map<String, Object>> unregister(@RequestParam String userId, @RequestParam String accountNo) {
        boolean removed = useCase.unregister(userId, accountNo);
        return ApiResponse.ok(Map.of("unregistered", removed));
    }

    public record RegisterRequest(String userId, String accountNo, String alias) {}
}
