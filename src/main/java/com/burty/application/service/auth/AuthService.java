/**
 *
 *
 * <pre>
 * <b>Description  : 인증 애플리케이션 서비스 (AuthService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.auth
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
package com.burty.application.service.auth;

import com.burty.application.dto.auth.CurrentUserResponse;
import com.burty.application.port.in.auth.AuthUseCase;
import com.burty.config.BurtyAuthProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.user.repository.UserProfileRepository;
import com.burty.security.JwtTokenProvider;
import java.util.Arrays;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

  private final JwtTokenProvider jwtTokenProvider;
  private final BurtyAuthProperties burtyAuthProperties;
  private final Environment environment;
  private final UserProfileRepository userProfileRepository;

  @Override
  public String issueTestToken(String userId) {
    if (Arrays.asList(environment.getActiveProfiles()).contains("prod")
        || !burtyAuthProperties.isTestTokenEnabled()) {
      throw new BusinessException(
          ErrorCode.FORBIDDEN,
          "테스트용 JWT 발급이 비활성화되어 있습니다. (burty.auth.test-token-enabled, prod 프로파일)");
    }
    String effectiveUserId = userId != null ? userId : UUID.randomUUID().toString();
    return jwtTokenProvider.generateToken(effectiveUserId);
  }

  @Override
  public CurrentUserResponse currentUser(String userId) {
    boolean profileComplete;
    try {
      profileComplete = userProfileRepository.existsById(Long.parseLong(userId));
    } catch (NumberFormatException e) {
      profileComplete = false;
    }
    return new CurrentUserResponse(userId, profileComplete);
  }
}
