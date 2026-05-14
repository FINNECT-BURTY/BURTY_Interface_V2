package com.berty.adapter.in.web;

import com.berty.application.port.in.RegisteredAccountUseCase;
import com.berty.core.dto.response.ApiResponse;
import com.berty.security.AuthLevel;
import com.berty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/berty/registered-accounts")
@Tag(name = "BERTY Registered Accounts", description = "이체 등록 계좌 (해시+암호화+마스킹) 관리")
public class RegisteredAccountController {

    private final RegisteredAccountUseCase useCase;

    public RegisteredAccountController(RegisteredAccountUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @AuthLevel(RiskLevel.LEVEL_2)
    @Operation(summary = "이체 등록 계좌 추가", description = "계좌번호는 해시(인덱스), 암호화(보관), 마스킹(표시) 3중으로 저장됩니다.")
    public ApiResponse<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        var saved = useCase.register(request.userId(), request.accountNo(), request.alias());
        return ApiResponse.ok(Map.of(
                "registered", true,
                "accountNoHash", saved.getAccountNoHash(),
                "accountNoMasked", saved.getAccountNoMasked()
        ));
    }

    @GetMapping
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "등록 계좌 목록 (마스킹)", description = "표시용 마스킹 계좌만 반환합니다.")
    public ApiResponse<List<MaskedView>> list(@RequestParam String userId) {
        return ApiResponse.ok(useCase.list(userId).stream()
                .map(v -> new MaskedView(v.accountNoHash(), v.accountNoMasked(), v.alias()))
                .toList());
    }

    @GetMapping("/decrypted")
    @AuthLevel(RiskLevel.LEVEL_3)
    @Operation(summary = "등록 계좌 평문 조회 (LEVEL_3)", description = "암호 복호화. 송금 직전 화면 등 LEVEL_3 인증 필요.")
    public ApiResponse<List<RegisteredAccountUseCase.View>> listDecrypted(@RequestParam String userId) {
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

    public record MaskedView(String accountNoHash, String accountNoMasked, String alias) {}
}
