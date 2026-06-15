/**
 *
 *
 * <pre>
 * <b>Description  : 관리 API 컨트롤러 (AiTemplateAdminController)</b>
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

import com.burty.application.dto.admin.AiFallbackTemplateRequest;
import com.burty.application.dto.admin.AiFallbackTemplateResponse;
import com.burty.application.dto.shared.SimpleResultResponse;
import com.burty.application.port.in.admin.AiTemplateAdminUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/ai-templates")
@RequiredArgsConstructor
@Tag(name = "BURTY AI Template Admin", description = "AI fallback 문구 템플릿 관리 API")
public class AiTemplateAdminController extends BaseController {

  private final AiTemplateAdminUseCase aiTemplateAdminUseCase;

  @GetMapping
  @AuthLevel(RiskLevel.LEVEL_3)
  public ApiResponse<List<AiFallbackTemplateResponse>> templates() {
    return ApiResponse.ok(aiTemplateAdminUseCase.listTemplates());
  }

  @PostMapping
  @AuthLevel(RiskLevel.LEVEL_3)
  public ApiResponse<AiFallbackTemplateResponse> upsert(
      @RequestBody AiFallbackTemplateRequest request) {
    return ApiResponse.ok(aiTemplateAdminUseCase.upsert(request));
  }

  @DeleteMapping("/{templateKey}")
  @AuthLevel(RiskLevel.LEVEL_3)
  public ApiResponse<SimpleResultResponse> deactivate(@PathVariable String templateKey) {
    aiTemplateAdminUseCase.deactivate(templateKey);
    return ApiResponse.ok(new SimpleResultResponse(true, "AI fallback 템플릿이 비활성화되었습니다."));
  }
}
