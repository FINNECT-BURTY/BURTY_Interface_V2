package com.berty.adapter.in.web;

import com.berty.application.port.in.TransactionSyncUseCase;
import com.berty.core.dto.response.ApiResponse;
import com.berty.domain.entity.TransactionEntity;
import com.berty.security.AuthLevel;
import com.berty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/berty/transactions")
@Tag(name = "BERTY Transactions", description = "거래내역 동기화·조회·재분류 API")
public class TransactionController {

    private final TransactionSyncUseCase transactionSyncUseCase;

    public TransactionController(TransactionSyncUseCase transactionSyncUseCase) {
        this.transactionSyncUseCase = transactionSyncUseCase;
    }

    @PostMapping("/sync")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "오픈뱅킹 거래내역 동기화", description = "fintechUseNum 기준으로 거래내역을 적재하고 카테고리를 자동 분류합니다.")
    public ApiResponse<Map<String, Object>> sync(@RequestParam String userId, @RequestParam String fintechUseNum) {
        int saved = transactionSyncUseCase.syncFromOpenBanking(userId, fintechUseNum);
        return ApiResponse.ok(Map.of("saved", saved, "userId", userId, "fintechUseNum", fintechUseNum));
    }

    @GetMapping
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "거래내역 조회", description = "기간(from, to) 미지정 시 최근 3개월")
    public ApiResponse<List<TransactionResponse>> list(@RequestParam String userId,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<TransactionResponse> items = transactionSyncUseCase.recent(userId, from, to).stream()
                .map(TransactionResponse::from)
                .toList();
        return ApiResponse.ok(items);
    }

    @PostMapping("/recategorize")
    @AuthLevel(RiskLevel.LEVEL_2)
    @Operation(summary = "전체 거래 재분류", description = "현재 활성 카테고리 룰을 모든 거래에 다시 적용합니다.")
    public ApiResponse<Map<String, Object>> recategorize(@RequestParam String userId) {
        int changed = transactionSyncUseCase.recategorizeAll(userId);
        return ApiResponse.ok(Map.of("changed", changed, "userId", userId));
    }

    public record TransactionResponse(String txId, LocalDate txnDate, long amount, String direction, String merchant, String memo, String expenseCategoryCode, String incomeCategoryCode, String source, Double categoryConfidence) {
        public static TransactionResponse from(TransactionEntity e) {
            return new TransactionResponse(
                    e.getTxId() == null ? null : e.getTxId().toString(),
                    e.getTxnDate(),
                    e.getAmount(),
                    e.getDirection(),
                    e.getMerchant(),
                    e.getMemo(),
                    e.getExpenseCategoryCode(),
                    e.getIncomeCategoryCode(),
                    e.getSource(),
                    e.getCategoryConfidence()
            );
        }
    }
}
