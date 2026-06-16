/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 API 컨트롤러 (UserProfileController)</b>
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

import com.burty.application.dto.user.UserNameResponse;
import com.burty.application.port.in.user.UserProfileUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(name = "User Profile", description = "사용자 프로필 조회")
@RequiredArgsConstructor
public class UserProfileController extends BaseController {

  private final UserProfileUseCase userProfileUseCase;

  @GetMapping("/me/name")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(
      summary = "내 이름 조회",
      description = "로그인한 사용자의 이름(실명)을 반환합니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  public ApiResponse<UserNameResponse> getMyName() {
    Long userId =
        Long.parseLong(
            String.valueOf(SecurityContextHolder.getContext().getAuthentication().getPrincipal()));
    String name = userProfileUseCase.getUserName(userId);
    return ApiResponse.ok(new UserNameResponse(name));
  }
}
