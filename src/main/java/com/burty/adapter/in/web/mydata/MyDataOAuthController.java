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

import com.burty.adapter.in.web.social.OAuthFrontendRedirect;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
  private final MyDataLinkRedirect linkRedirect;
  private final OAuthFrontendRedirect frontendRedirect;

  @GetMapping("/mydata/oauth/authorize")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<AuthorizeUrlResponse> authorizeMyData(
      @CurrentUserId String userId, HttpServletRequest request) {
    // 연결을 마치고 돌아갈 FE 를 인가 요청에 묶어 둔다.
    String authorizeUrl =
        myDataAuthUseCase.createAuthorizeUrl(
            userId, null, frontendRedirect.resolveFromRequest(request));
    return ApiResponse.ok(new AuthorizeUrlResponse(authorizeUrl));
  }

  @GetMapping("/mydata/oauth/callback")
  @Operation(summary = "마이데이터 OAuth redirect 콜백")
  public ResponseEntity<Void> myDataCallbackRedirect(
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String error,
      HttpServletRequest request) {
    // 사용자가 거절하면 정보제공자는 code 없이 error 만 준다. 그것도 결과로 FE 에 돌려보낸다.
    return linkRedirect.to(myDataAuthUseCase.completeAuthorization(state, code, error), request);
  }

  @PostMapping("/mydata/oauth/callback")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<FlagResultResponse> myDataCallback(
      @CurrentUserId String userId, @Valid @RequestBody MyDataCallbackRequest request) {
    boolean linked = myDataAuthUseCase.exchangeAuthorizationCode(userId, request.code());
    return ApiResponse.ok(FlagResultResponse.of("linked", linked));
  }
}
