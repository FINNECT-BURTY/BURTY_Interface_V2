/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 애플리케이션 서비스 (UserProfileService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.user
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
package com.burty.application.service.user;

import com.burty.application.port.in.user.UserProfileUseCase;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.user.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService implements UserProfileUseCase {

  private final UserProfileRepository userProfileRepository;

  public UserProfileService(UserProfileRepository userProfileRepository) {
    this.userProfileRepository = userProfileRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public String getUserName(Long userId) {
    return userProfileRepository
        .findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "사용자 프로필을 찾을 수 없습니다."))
        .getName();
  }
}
