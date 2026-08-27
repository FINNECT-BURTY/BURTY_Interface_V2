/**
 *
 *
 * <pre>
 * <b>Description  : 마이데이터 API 컨트롤러 (MyDataOAuthController)</b>
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

import com.burty.application.dto.auth.AuthorizeUrlResponse;
import com.burty.application.dto.mydata.MyDataCallbackRequest;
import com.burty.application.dto.shared.FlagResultResponse;
import com.burty.application.port.in.mydata.MyDataAuthUseCase;
import com.burty.core.annotation.CurrentUserId;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "BURTY MyData OAuth", description = "마이데이터 OAuth 연동 API")
public class MyDataOAuthController extends BaseController {

  private final MyDataAuthUseCase myDataAuthUseCase;

  @GetMapping("/mydata/oauth/authorize")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<AuthorizeUrlResponse> authorizeMyData(@CurrentUserId String userId) {
    String authorizeUrl = myDataAuthUseCase.createAuthorizeUrl(userId);
    return ApiResponse.ok(new AuthorizeUrlResponse(authorizeUrl));
  }

  @GetMapping("/mydata/oauth/callback")
  @Operation(summary = "마이데이터 OAuth redirect 콜백")
  public ApiResponse<FlagResultResponse> myDataCallbackRedirect(
      @RequestParam String code, @RequestParam String state) {
    boolean linked = myDataAuthUseCase.exchangeAuthorizationCodeByState(state, code);
    return ApiResponse.ok(FlagResultResponse.of("linked", linked));
  }

  @PostMapping("/mydata/oauth/callback")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<FlagResultResponse> myDataCallback(
      @CurrentUserId String userId, @Valid @RequestBody MyDataCallbackRequest request) {
    boolean linked = myDataAuthUseCase.exchangeAuthorizationCode(userId, request.code());
    return ApiResponse.ok(FlagResultResponse.of("linked", linked));
  }
}
