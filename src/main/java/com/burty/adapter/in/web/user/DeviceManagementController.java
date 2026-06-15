/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 API 컨트롤러 (DeviceManagementController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.user
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
package com.burty.adapter.in.web.user;

import com.burty.application.dto.shared.SimpleResultResponse;
import com.burty.application.dto.user.DeviceNameUpdateRequest;
import com.burty.application.dto.user.DeviceResponse;
import com.burty.application.port.in.user.DeviceManagementUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
@Tag(name = "BURTY Device Management", description = "신뢰 디바이스 조회/이름 변경/해제 API")
public class DeviceManagementController extends BaseController {

  private final DeviceManagementUseCase deviceManagementUseCase;

  @GetMapping
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "등록 기기 목록", description = "사용자에게 등록된 신뢰 기기 목록을 조회합니다.")
  public ApiResponse<List<DeviceResponse>> devices(@RequestParam String userId) {
    return ApiResponse.ok(deviceManagementUseCase.listDevices(userId));
  }

  @PatchMapping("/{deviceId}/name")
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "기기명 변경", description = "등록된 기기의 표시 이름을 변경합니다.")
  public ApiResponse<DeviceResponse> updateName(
      @PathVariable String deviceId, @RequestBody DeviceNameUpdateRequest request) {
    return ApiResponse.ok(deviceManagementUseCase.updateDeviceName(deviceId, request));
  }

  @DeleteMapping("/{deviceId}")
  @AuthLevel(RiskLevel.LEVEL_3)
  @Operation(summary = "기기 해제", description = "분실/교체된 신뢰 기기를 해제하고 연결된 생체 credential도 폐기합니다.")
  public ApiResponse<SimpleResultResponse> revoke(
      @PathVariable String deviceId, @RequestParam String userId) {
    deviceManagementUseCase.revokeDevice(deviceId, userId);
    return ApiResponse.ok(new SimpleResultResponse(true, "기기가 해제되었습니다."));
  }
}
