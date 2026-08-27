package com.burty.adapter.in.web.family;

import com.burty.application.service.family.TransferApprovalService;
import com.burty.core.annotation.CurrentUserId;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.domain.family.entity.TransferApprovalEntity;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이체 보호자 사전 승인 API.
 *
 * <p>기존 가족 보호 기능은 이상 이체를 사후 통지만 했다. 알림이 갔을 때는 이미 출금이 끝난 뒤라 피해를 막지 못한다. 이 API 는 고액 이체를 <b>보류</b>하고
 * 보호자가 직접 승인/거절하게 한다.
 */
@RestController
@RequestMapping("/family/approvals")
@Tag(name = "BURTY Family Approval", description = "이체 보호자 사전 승인 API")
@RequiredArgsConstructor
public class TransferApprovalController extends BaseController {

  private final TransferApprovalService approvalService;

  public record DecisionRequest(@Size(max = 200, message = "사유는 200자를 넘을 수 없습니다") String note) {}

  public record ApprovalResponse(
      Long approvalId,
      Long orderId,
      String requesterUserId,
      long amount,
      String toAccountMasked,
      String status,
      String requestedAt,
      String expiresAt) {

    static ApprovalResponse from(TransferApprovalEntity e) {
      return new ApprovalResponse(
          e.getApprovalId(),
          e.getOrderId(),
          e.getRequesterUserId(),
          e.getAmount(),
          e.getToAccountMasked(),
          e.getStatus().name(),
          String.valueOf(e.getRequestedAt()),
          String.valueOf(e.getExpiresAt()));
    }
  }

  @GetMapping("/pending")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "승인 대기 목록", description = "내가 보호자로 지정된 계정의 승인 대기 이체를 조회합니다.")
  public ApiResponse<List<ApprovalResponse>> pending(@CurrentUserId String guardianUserId) {
    return ApiResponse.ok(
        approvalService.pendingForGuardian(guardianUserId).stream()
            .map(ApprovalResponse::from)
            .toList());
  }

  @GetMapping("/mine")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "내 승인 요청 이력", description = "내가 요청해 보호자 승인을 기다린 이체 내역입니다.")
  public ApiResponse<List<ApprovalResponse>> mine(@CurrentUserId String userId) {
    return ApiResponse.ok(
        approvalService.myRequests(userId).stream().map(ApprovalResponse::from).toList());
  }

  /**
   * 승인.
   *
   * <p>승인 후 실제 이체 실행은 별도 확인 단계를 거친다. 보호자가 승인했다고 해서 피보호자의 생체인증 없이 돈이 나가면 안 되기 때문이다. 승인은 "실행해도 좋다" 는
   * 허가이지 인증의 대체가 아니다.
   */
  @PostMapping("/{approvalId}/approve")
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "이체 승인")
  public ApiResponse<ApprovalResponse> approve(
      @CurrentUserId String guardianUserId,
      @PathVariable Long approvalId,
      @Valid @RequestBody DecisionRequest request) {
    return ApiResponse.ok(
        ApprovalResponse.from(approvalService.approve(guardianUserId, approvalId, request.note())));
  }

  @PostMapping("/{approvalId}/reject")
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "이체 거절")
  public ApiResponse<ApprovalResponse> reject(
      @CurrentUserId String guardianUserId,
      @PathVariable Long approvalId,
      @Valid @RequestBody DecisionRequest request) {
    return ApiResponse.ok(
        ApprovalResponse.from(approvalService.reject(guardianUserId, approvalId, request.note())));
  }
}
