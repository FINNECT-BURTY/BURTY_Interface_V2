/**
 *
 *
 * <pre>
 * <b>Description  : 금융 API 컨트롤러 (FinanceTransferController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.finance
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
package com.burty.adapter.in.web.finance;

import com.burty.adapter.in.web.mapper.WebResponseMapper;
import com.burty.application.dto.finance.LimitResponse;
import com.burty.application.dto.finance.LimitUpdateRequest;
import com.burty.application.dto.finance.LimitUpdateResponse;
import com.burty.application.dto.finance.TransferDetailResponse;
import com.burty.application.dto.finance.TransferRequest;
import com.burty.application.dto.finance.TransferResponse;
import com.burty.application.port.in.finance.TransferUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.domain.finance.model.TransferResult;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "BURTY Transfer", description = "이체 및 한도 설정 API")
public class FinanceTransferController extends BaseController {

  private final TransferUseCase transferUseCase;
  private final WebResponseMapper webResponseMapper;

  @PostMapping("/settings/limits")
  @AuthLevel(RiskLevel.LEVEL_2)
  public ApiResponse<LimitUpdateResponse> updateLimit(@RequestBody LimitUpdateRequest request) {
    transferUseCase.updateLimit(request.userId(), request.limit());
    return ApiResponse.ok(new LimitUpdateResponse(true, request.limit()));
  }

  @GetMapping("/settings/limits")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<LimitResponse> getLimit(@RequestParam String userId) {
    return ApiResponse.ok(new LimitResponse(userId, transferUseCase.getLimit(userId)));
  }

  @PostMapping("/transfers")
  @AuthLevel(RiskLevel.LEVEL_3)
  public ApiResponse<TransferResponse> transfer(@RequestBody TransferRequest request) {
    TransferResult result =
        transferUseCase.transfer(
            request.userId(),
            request.fromAccount(),
            request.toAccount(),
            request.amount(),
            request.description(),
            request.assertionToken(),
            request.idempotencyKey());
    return ApiResponse.ok(webResponseMapper.toResponse(result));
  }

  @GetMapping("/transfers/{transferId}")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<TransferDetailResponse> getTransfer(@PathVariable String transferId) {
    TransferResult result = transferUseCase.getTransfer(transferId);
    if (result == null) {
      return ApiResponse.ok(TransferDetailResponse.notFound(transferId));
    }
    return ApiResponse.ok(
        TransferDetailResponse.found(
            result.transferId(), result.status(), result.familyNotified()));
  }

  @GetMapping("/transfers")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<List<TransferResponse>> transfers(@RequestParam String userId) {
    return ApiResponse.ok(
        webResponseMapper.toTransferResponses(transferUseCase.getTransfers(userId)));
  }
}
