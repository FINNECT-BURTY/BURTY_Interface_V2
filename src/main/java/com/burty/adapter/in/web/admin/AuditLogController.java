/**
 *
 *
 * <pre>
 * <b>Description  : 관리 API 컨트롤러 (AuditLogController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.admin
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
package com.burty.adapter.in.web.admin;

import com.burty.application.dto.admin.AuditLogResponse;
import com.burty.application.port.in.admin.AuditLogUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
@Tag(name = "BURTY Audit Logs", description = "로그인/생체/마이데이터/정책/추천 실행 감사로그 조회 API")
public class AuditLogController extends BaseController {

  private final AuditLogUseCase auditLogUseCase;

  @GetMapping
  @AuthLevel(RiskLevel.LEVEL_3)
  public ApiResponse<List<AuditLogResponse>> logs(@RequestParam(defaultValue = "50") int size) {
    return ApiResponse.ok(auditLogUseCase.listRecent(size));
  }
}
