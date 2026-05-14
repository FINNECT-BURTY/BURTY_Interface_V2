package com.berty.adapter.in.web;

import com.berty.application.port.in.MyDataAuthUseCase;
import com.berty.core.dto.response.ApiResponse;
import com.berty.domain.entity.MyDataLinkStatusEntity;
import com.berty.security.AuthLevel;
import com.berty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/berty/mydata/institutions")
@Tag(name = "BERTY MyData Institutions", description = "MyData 기관 다중 연동 관리 API")
public class MyDataInstitutionController {

    private final MyDataAuthUseCase myDataAuthUseCase;

    public MyDataInstitutionController(MyDataAuthUseCase myDataAuthUseCase) {
        this.myDataAuthUseCase = myDataAuthUseCase;
    }

    @GetMapping
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "연동된 기관 목록", description = "사용자의 모든 MyData 연동 기관 상태를 반환합니다.")
    public ApiResponse<List<InstitutionResponse>> list(@RequestParam String userId) {
        List<InstitutionResponse> items = myDataAuthUseCase.listInstitutions(userId).stream()
                .map(InstitutionResponse::from)
                .toList();
        return ApiResponse.ok(items);
    }

    @GetMapping("/{institutionCode}/authorize")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "기관별 OAuth 인가 URL", description = "특정 기관 코드에 대한 인가 URL을 발급합니다.")
    public ApiResponse<Map<String, String>> authorize(@RequestParam String userId, @PathVariable String institutionCode) {
        return ApiResponse.ok(Map.of(
                "authorizeUrl", myDataAuthUseCase.createAuthorizeUrl(userId, institutionCode),
                "institutionCode", institutionCode
        ));
    }

    @PostMapping("/{institutionCode}/callback")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "기관별 OAuth 콜백", description = "기관 코드를 명시한 토큰 교환 처리.")
    public ApiResponse<Map<String, Object>> callback(@RequestParam String userId,
                                                     @PathVariable String institutionCode,
                                                     @RequestBody CallbackRequest request) {
        boolean linked = myDataAuthUseCase.exchangeAuthorizationCode(userId, institutionCode, request.code());
        return ApiResponse.ok(Map.of("linked", linked, "institutionCode", institutionCode));
    }

    @DeleteMapping("/{institutionCode}")
    @AuthLevel(RiskLevel.LEVEL_2)
    @Operation(summary = "기관 연동 해지", description = "특정 기관의 status를 UNLINKED로 마킹합니다.")
    public ApiResponse<Map<String, Object>> unlink(@RequestParam String userId, @PathVariable String institutionCode) {
        boolean ok = myDataAuthUseCase.unlinkInstitution(userId, institutionCode);
        return ApiResponse.ok(Map.of("unlinked", ok, "institutionCode", institutionCode));
    }

    public record CallbackRequest(String code) {}

    public record InstitutionResponse(
            String institutionCode,
            String status,
            LocalDateTime linkedAt,
            LocalDateTime tokenExpiresAt,
            LocalDateTime unlinkedAt,
            String lastErrorCode,
            LocalDateTime lastErrorAt
    ) {
        public static InstitutionResponse from(MyDataLinkStatusEntity e) {
            return new InstitutionResponse(
                    e.getInstitutionCode(), e.getStatus(),
                    e.getLinkedAt(), e.getTokenExpiresAt(),
                    e.getUnlinkedAt(), e.getLastErrorCode(), e.getLastErrorAt()
            );
        }
    }
}
