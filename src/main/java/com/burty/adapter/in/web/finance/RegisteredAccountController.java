/**
 *
 *
 * <pre>
 * <b>Description  : 금융 API 컨트롤러 (RegisteredAccountController)</b>
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

import com.burty.application.dto.finance.RegisteredAccountRegisterResponse;
import com.burty.application.dto.shared.FlagResultResponse;
import com.burty.application.port.in.finance.RegisteredAccountUseCase;
import com.burty.core.annotation.CurrentUserId;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/registered-accounts")
@Tag(name = "BURTY Registered Accounts", description = "이체 등록 계좌 관리")
@RequiredArgsConstructor
public class RegisteredAccountController extends BaseController {

  private final RegisteredAccountUseCase useCase;

  @PostMapping
  @AuthLevel(RiskLevel.LEVEL_3)
  @Operation(summary = "이체 등록 계좌 추가", description = "계좌번호를 입력한 그대로 저장합니다.")
  public ApiResponse<RegisteredAccountRegisterResponse> register(
      @CurrentUserId String userId, @Valid @RequestBody RegisterRequest request) {
    var saved = useCase.register(userId, request.accountNo(), request.alias());
    return ApiResponse.ok(new RegisteredAccountRegisterResponse(true, saved.getAccountNo()));
  }

  @GetMapping
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "등록 계좌 목록")
  public ApiResponse<List<RegisteredAccountUseCase.View>> list(@CurrentUserId String userId) {
    return ApiResponse.ok(useCase.list(userId));
  }

  @DeleteMapping
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "등록 계좌 해제")
  public ApiResponse<FlagResultResponse> unregister(
      @CurrentUserId String userId, @RequestParam String accountNo) {
    boolean removed = useCase.unregister(userId, accountNo);
    return ApiResponse.ok(FlagResultResponse.of("unregistered", removed));
  }

  public record RegisterRequest(String userId, String accountNo, String alias) {}
}
