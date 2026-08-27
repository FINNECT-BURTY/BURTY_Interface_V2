package com.burty.adapter.in.web.mydata;

import com.burty.application.dto.mydata.MyDataConsentHistoryResponse;
import com.burty.application.dto.mydata.MyDataTransmissionLogResponse;
import com.burty.application.dto.mydata.TransmissionRequestCreateRequest;
import com.burty.application.dto.mydata.TransmissionRequestResponse;
import com.burty.application.dto.shared.FlagResultResponse;
import com.burty.application.port.in.mydata.MyDataTransmissionUseCase;
import com.burty.core.annotation.CurrentUserId;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mydata/transmission")
@RequiredArgsConstructor
@Tag(name = "BURTY MyData Transmission", description = "마이데이터 전송요구·동의·감사로그 API")
public class MyDataTransmissionController extends BaseController {

  private final MyDataTransmissionUseCase transmissionUseCase;

  @PostMapping("/requests")
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "정보전송요구 생성")
  public ApiResponse<TransmissionRequestResponse> createRequest(
      @CurrentUserId String userId, @Valid @RequestBody TransmissionRequestCreateRequest request) {
    return ApiResponse.ok(
        TransmissionRequestResponse.from(
            transmissionUseCase.createRequest(userId, request.institutionCode(), request.scope())));
  }

  @GetMapping("/requests")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "정보전송요구 목록")
  public ApiResponse<List<TransmissionRequestResponse>> listRequests(@CurrentUserId String userId) {
    return ApiResponse.ok(
        transmissionUseCase.listRequests(userId).stream()
            .map(TransmissionRequestResponse::from)
            .toList());
  }

  @PostMapping("/requests/{requestId}/revoke")
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "정보전송요구 철회")
  public ApiResponse<FlagResultResponse> revokeRequest(
      @PathVariable Long requestId,
      @CurrentUserId String userId,
      @RequestParam(defaultValue = "사용자 철회") String reason) {
    boolean ok = transmissionUseCase.revokeRequest(userId, requestId, reason);
    return ApiResponse.ok(FlagResultResponse.of("revoked", ok));
  }

  @GetMapping("/consents")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "마이데이터 동의 이력")
  public ApiResponse<List<MyDataConsentHistoryResponse>> listConsents(
      @CurrentUserId String userId) {
    return ApiResponse.ok(
        transmissionUseCase.listConsents(userId).stream()
            .map(MyDataConsentHistoryResponse::from)
            .toList());
  }

  @GetMapping("/logs")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "전송 감사 로그")
  public ApiResponse<List<MyDataTransmissionLogResponse>> listLogs(
      @CurrentUserId String userId, @RequestParam(defaultValue = "30") int days) {
    return ApiResponse.ok(
        transmissionUseCase.listTransmissionLogs(userId, days).stream()
            .map(MyDataTransmissionLogResponse::from)
            .toList());
  }

  @PostMapping("/accounts/sync")
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "오픈뱅킹 계좌 동기화")
  public ApiResponse<Map<String, Object>> syncAccounts(@CurrentUserId String userId) {
    int saved = transmissionUseCase.syncAccounts(userId);
    return ApiResponse.ok(Map.of("userId", userId, "savedAccounts", saved));
  }
}
