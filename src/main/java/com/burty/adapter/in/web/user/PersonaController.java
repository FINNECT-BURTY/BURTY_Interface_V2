/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 API 컨트롤러 (PersonaController)</b>
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

import com.burty.adapter.in.web.mapper.WebResponseMapper;
import com.burty.application.dto.user.PersonaResponse;
import com.burty.application.port.in.user.PersonaInferenceUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/persona")
@RequiredArgsConstructor
@Tag(name = "BURTY Persona", description = "사용자 페르소나 (직업/거주/소득) 조회·수정 API")
public class PersonaController extends BaseController {

  private final PersonaInferenceUseCase personaInferenceUseCase;
  private final WebResponseMapper webResponseMapper;

  @GetMapping("/{userId}")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "페르소나 조회/추론", description = "저장된 페르소나가 없으면 자동 추론 후 반환합니다.")
  public ApiResponse<PersonaResponse> get(@PathVariable String userId) {
    return ApiResponse.ok(webResponseMapper.toResponse(personaInferenceUseCase.getOrInfer(userId)));
  }

  @PutMapping("/{userId}")
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "페르소나 수정", description = "사용자가 직업/거주/세대/소득을 직접 지정합니다.")
  public ApiResponse<PersonaResponse> override(
      @PathVariable String userId, @RequestBody OverrideRequest request) {
    return ApiResponse.ok(
        webResponseMapper.toResponse(
            personaInferenceUseCase.overrideByUser(
                userId,
                request.getOccupationCode(),
                request.getResidenceCode(),
                request.getHouseholdType(),
                request.getMonthlyIncomeAvg())));
  }

  @PostMapping("/{userId}/reinfer")
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "페르소나 재추론", description = "현재 자산 스냅샷 기반으로 다시 추론합니다.")
  public ApiResponse<PersonaResponse> reinfer(@PathVariable String userId) {
    return ApiResponse.ok(webResponseMapper.toResponse(personaInferenceUseCase.reinfer(userId)));
  }

  public static class OverrideRequest {
    private String occupationCode;
    private String residenceCode;
    private String householdType;
    private Long monthlyIncomeAvg;

    public String getOccupationCode() {
      return occupationCode;
    }

    public void setOccupationCode(String occupationCode) {
      this.occupationCode = occupationCode;
    }

    public String getResidenceCode() {
      return residenceCode;
    }

    public void setResidenceCode(String residenceCode) {
      this.residenceCode = residenceCode;
    }

    public String getHouseholdType() {
      return householdType;
    }

    public void setHouseholdType(String householdType) {
      this.householdType = householdType;
    }

    public Long getMonthlyIncomeAvg() {
      return monthlyIncomeAvg;
    }

    public void setMonthlyIncomeAvg(Long monthlyIncomeAvg) {
      this.monthlyIncomeAvg = monthlyIncomeAvg;
    }
  }
}
