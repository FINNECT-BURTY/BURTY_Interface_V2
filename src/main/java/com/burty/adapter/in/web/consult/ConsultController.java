/**
 *
 *
 * <pre>
 * <b>Description  : 상담 API 컨트롤러 (ConsultController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.consult
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
package com.burty.adapter.in.web.consult;

import com.burty.adapter.in.web.mapper.WebResponseMapper;
import com.burty.application.dto.consult.ConsultRequest;
import com.burty.application.dto.consult.ConsultResponse;
import com.burty.application.dto.consult.MonthlyReportResponse;
import com.burty.application.port.in.consult.AiAdvisoryUseCase;
import com.burty.application.port.in.consult.ConsultUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "BURTY Consult", description = "금융 상담 및 월간 리포트 API")
public class ConsultController extends BaseController {

  private final ConsultUseCase consultUseCase;
  private final AiAdvisoryUseCase aiAdvisoryUseCase;
  private final WebResponseMapper webResponseMapper;

  @PostMapping("/consult")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<ConsultResponse> consult(@RequestBody ConsultRequest request) {
    return ApiResponse.ok(
        webResponseMapper.toResponse(consultUseCase.consult(request.userId(), request.question())));
  }

  @PostMapping("/ai/consult")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "AI 상담", description = "OpenAI 기반 자산 상담 결과를 쉬운 말로 제공합니다.")
  public ApiResponse<ConsultResponse> aiConsult(@RequestBody ConsultRequest request) {
    return ApiResponse.ok(
        webResponseMapper.toResponse(
            aiAdvisoryUseCase.consultWithAi(request.userId(), request.question())));
  }

  @GetMapping("/reports/monthly")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<MonthlyReportResponse> monthlyReport(@RequestParam String userId) {
    return ApiResponse.ok(webResponseMapper.toResponse(consultUseCase.createMonthlyReport(userId)));
  }
}
