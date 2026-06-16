/**
 *
 *
 * <pre>
 * <b>Description  : 거래 API 컨트롤러 (TransactionController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.transaction
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
package com.burty.adapter.in.web.transaction;

import com.burty.application.dto.transaction.TransactionChangeResponse;
import com.burty.application.dto.transaction.TransactionResponse;
import com.burty.application.dto.transaction.TransactionSyncResponse;
import com.burty.application.port.in.transaction.TransactionSyncUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@Tag(name = "BURTY Transactions", description = "거래내역 동기화·조회·재분류 API")
@RequiredArgsConstructor
public class TransactionController extends BaseController {

  private final TransactionSyncUseCase transactionSyncUseCase;

  @PostMapping("/sync")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(
      summary = "오픈뱅킹 거래내역 동기화",
      description = "fintechUseNum 기준으로 거래내역을 적재하고 카테고리를 자동 분류합니다.")
  public ApiResponse<TransactionSyncResponse> sync(
      @RequestParam String userId, @RequestParam String fintechUseNum) {
    int saved = transactionSyncUseCase.syncFromOpenBanking(userId, fintechUseNum);
    return ApiResponse.ok(new TransactionSyncResponse(saved, userId, fintechUseNum));
  }

  @GetMapping
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "거래내역 조회", description = "기간(from, to) 미지정 시 최근 3개월")
  public ApiResponse<List<TransactionResponse>> list(
      @RequestParam String userId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    List<TransactionResponse> items =
        transactionSyncUseCase.recent(userId, from, to).stream()
            .map(TransactionResponse::from)
            .toList();
    return ApiResponse.ok(items);
  }

  @PostMapping("/recategorize")
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "전체 거래 재분류", description = "현재 활성 카테고리 룰을 모든 거래에 다시 적용합니다.")
  public ApiResponse<TransactionChangeResponse> recategorize(@RequestParam String userId) {
    int changed = transactionSyncUseCase.recategorizeAll(userId);
    return ApiResponse.ok(new TransactionChangeResponse(changed, userId));
  }
}
