/**
 *
 *
 * <pre>
 * <b>Description  : 마이데이터 API 컨트롤러 (MyDataInstitutionController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.mydata
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
package com.burty.adapter.in.web.mydata;

import com.burty.application.dto.mydata.InstitutionResponse;
import com.burty.application.dto.mydata.InstitutionResultResponse;
import com.burty.application.dto.mydata.MyDataAuthorizeResponse;
import com.burty.application.dto.shared.FlagResultResponse;
import com.burty.application.port.in.mydata.MyDataAuthUseCase;
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
@RequestMapping("/mydata/institutions")
@Tag(name = "BURTY MyData Institutions", description = "MyData 기관 다중 연동 관리 API")
@RequiredArgsConstructor
public class MyDataInstitutionController extends BaseController {

  private final MyDataAuthUseCase myDataAuthUseCase;

  @GetMapping
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "연동된 기관 목록", description = "사용자의 모든 MyData 연동 기관 상태를 반환합니다.")
  public ApiResponse<List<InstitutionResponse>> list(@RequestParam String userId) {
    List<InstitutionResponse> items =
        myDataAuthUseCase.listInstitutions(userId).stream().map(InstitutionResponse::from).toList();
    return ApiResponse.ok(items);
  }

  @GetMapping("/{institutionCode}/authorize")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "기관별 OAuth 인가 URL", description = "특정 기관 코드에 대한 인가 URL을 발급합니다.")
  public ApiResponse<MyDataAuthorizeResponse> authorize(
      @RequestParam String userId, @PathVariable String institutionCode) {
    return ApiResponse.ok(
        new MyDataAuthorizeResponse(
            myDataAuthUseCase.createAuthorizeUrl(userId, institutionCode), institutionCode));
  }

  @GetMapping("/{institutionCode}/callback")
  @Operation(summary = "기관별 OAuth redirect 콜백")
  public ApiResponse<FlagResultResponse> callbackRedirect(
      @PathVariable String institutionCode, @RequestParam String code, @RequestParam String state) {
    boolean linked = myDataAuthUseCase.exchangeAuthorizationCodeByState(state, code);
    return ApiResponse.ok(FlagResultResponse.of("linked", linked));
  }

  @PostMapping("/{institutionCode}/callback")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "기관별 OAuth 콜백", description = "기관 코드를 명시한 토큰 교환 처리.")
  public ApiResponse<FlagResultResponse> callback(
      @RequestParam String userId,
      @PathVariable String institutionCode,
      @RequestBody CallbackRequest request) {
    boolean linked =
        myDataAuthUseCase.exchangeAuthorizationCode(userId, institutionCode, request.code());
    return ApiResponse.ok(FlagResultResponse.of("linked", linked));
  }

  @DeleteMapping("/{institutionCode}")
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "기관 연동 해지", description = "특정 기관의 status를 UNLINKED로 마킹합니다.")
  public ApiResponse<InstitutionResultResponse> unlink(
      @RequestParam String userId, @PathVariable String institutionCode) {
    boolean ok = myDataAuthUseCase.unlinkInstitution(userId, institutionCode);
    return ApiResponse.ok(new InstitutionResultResponse(ok, institutionCode));
  }

  public record CallbackRequest(String code) {}
}
