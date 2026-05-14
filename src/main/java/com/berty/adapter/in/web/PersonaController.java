package com.berty.adapter.in.web;

import com.berty.application.port.in.PersonaInferenceUseCase;
import com.berty.core.dto.response.ApiResponse;
import com.berty.domain.entity.PersonaProfileEntity;
import com.berty.security.AuthLevel;
import com.berty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/berty/persona")
@Tag(name = "BERTY Persona", description = "사용자 페르소나 (직업/거주/소득) 조회·수정 API")
public class PersonaController {

    private final PersonaInferenceUseCase personaInferenceUseCase;

    public PersonaController(PersonaInferenceUseCase personaInferenceUseCase) {
        this.personaInferenceUseCase = personaInferenceUseCase;
    }

    @GetMapping("/{userId}")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "페르소나 조회/추론", description = "저장된 페르소나가 없으면 자동 추론 후 반환합니다.")
    public ApiResponse<PersonaResponse> get(@PathVariable String userId) {
        return ApiResponse.ok(PersonaResponse.from(personaInferenceUseCase.getOrInfer(userId)));
    }

    @PutMapping("/{userId}")
    @AuthLevel(RiskLevel.LEVEL_2)
    @Operation(summary = "페르소나 수정", description = "사용자가 직업/거주/세대/소득을 직접 지정합니다.")
    public ApiResponse<PersonaResponse> override(@PathVariable String userId, @RequestBody OverrideRequest request) {
        PersonaProfileEntity entity = personaInferenceUseCase.overrideByUser(
                userId,
                request.getOccupationCode(),
                request.getResidenceCode(),
                request.getHouseholdType(),
                request.getMonthlyIncomeAvg()
        );
        return ApiResponse.ok(PersonaResponse.from(entity));
    }

    @PostMapping("/{userId}/reinfer")
    @AuthLevel(RiskLevel.LEVEL_2)
    @Operation(summary = "페르소나 재추론", description = "현재 자산 스냅샷 기반으로 다시 추론합니다.")
    public ApiResponse<PersonaResponse> reinfer(@PathVariable String userId) {
        return ApiResponse.ok(PersonaResponse.from(personaInferenceUseCase.reinfer(userId)));
    }

    public record PersonaResponse(
            String userId,
            String occupationCode,
            String residenceCode,
            String householdType,
            Long monthlyIncomeAvg,
            Double incomeVariabilityPct,
            Integer age,
            String source,
            Boolean userOverridden,
            LocalDateTime inferredAt
    ) {
        public static PersonaResponse from(PersonaProfileEntity e) {
            return new PersonaResponse(
                    e.getUserId() == null ? null : e.getUserId().toString(),
                    e.getOccupationCode(),
                    e.getResidenceCode(),
                    e.getHouseholdType(),
                    e.getMonthlyIncomeAvg(),
                    e.getIncomeVariabilityPct(),
                    e.getAge(),
                    e.getSource(),
                    e.getUserOverridden(),
                    e.getInferredAt()
            );
        }
    }

    public static class OverrideRequest {
        private String occupationCode;
        private String residenceCode;
        private String householdType;
        private Long monthlyIncomeAvg;

        public String getOccupationCode() { return occupationCode; }
        public void setOccupationCode(String occupationCode) { this.occupationCode = occupationCode; }
        public String getResidenceCode() { return residenceCode; }
        public void setResidenceCode(String residenceCode) { this.residenceCode = residenceCode; }
        public String getHouseholdType() { return householdType; }
        public void setHouseholdType(String householdType) { this.householdType = householdType; }
        public Long getMonthlyIncomeAvg() { return monthlyIncomeAvg; }
        public void setMonthlyIncomeAvg(Long monthlyIncomeAvg) { this.monthlyIncomeAvg = monthlyIncomeAvg; }
    }
}
