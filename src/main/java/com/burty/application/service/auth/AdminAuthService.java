/**
 *
 *
 * <pre>
 * <b>Description  : 인증 애플리케이션 서비스 (AdminAuthService)</b>
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

import com.burty.application.port.in.auth.AdminAuthUseCase;
import com.burty.config.AdminProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.admin.entity.AdminUserEntity;
import com.burty.domain.admin.repository.AdminUserRepository;
import com.burty.security.JwtTokenProvider;
import com.burty.util.LoginFailLogUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuthService implements AdminAuthUseCase {

  private final AdminUserRepository adminUserRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;
  private final AdminProperties adminProperties;
  private final LoginFailLogUtil loginFailLogUtil;

  public AdminAuthService(
      AdminUserRepository adminUserRepository,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder,
      AdminProperties adminProperties,
      LoginFailLogUtil loginFailLogUtil) {
    this.adminUserRepository = adminUserRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.passwordEncoder = passwordEncoder;
    this.adminProperties = adminProperties;
    this.loginFailLogUtil = loginFailLogUtil;
  }

  @Override
  @Transactional(readOnly = true)
  public String login(String username, String password) {
    loginFailLogUtil.requireNotLocked(username, 5);

    AdminUserEntity admin =
        adminUserRepository
            .findByUsername(username)
            .orElseThrow(
                () -> {
                  loginFailLogUtil.logAdminFailure(username, "USER_NOT_FOUND");
                  return new BusinessException(ErrorCode.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
                });

    if (admin.getStatus() == AdminUserEntity.AdminStatus.SUSPENDED) {
      loginFailLogUtil.logAdminFailure(username, "ACCOUNT_SUSPENDED");
      throw new BusinessException(ErrorCode.FORBIDDEN, "정지된 관리자 계정입니다.");
    }

    if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
      loginFailLogUtil.logAdminFailure(username, "INVALID_PASSWORD");
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    loginFailLogUtil.clearFailLog(username);
    return jwtTokenProvider.generateToken(String.valueOf(admin.getAdminId()), "ROLE_ADMIN");
  }

  @Override
  @Transactional
  public void register(String setupKey, String username, String password, String roleName) {
    String configuredKey = adminProperties.getSetupKey();
    if (configuredKey == null || configuredKey.isBlank()) {
      throw new BusinessException(
          ErrorCode.FORBIDDEN, "관리자 등록이 비활성화되어 있습니다. (burty.admin.setup-key 미설정)");
    }
    if (!configuredKey.equals(setupKey)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "셋업 키가 올바르지 않습니다.");
    }
    if (adminUserRepository.existsByUsername(username)) {
      throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "이미 사용 중인 아이디입니다.");
    }
    if (password == null || password.length() < 8) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "비밀번호는 8자 이상이어야 합니다.");
    }

    AdminUserEntity admin = new AdminUserEntity();
    admin.setUsername(username);
    admin.setPasswordHash(passwordEncoder.encode(password));
    admin.setRole(parseRole(roleName));
    adminUserRepository.save(admin);
  }

  private AdminUserEntity.AdminRole parseRole(String roleName) {
    if ("SUPER_ADMIN".equalsIgnoreCase(roleName)) {
      return AdminUserEntity.AdminRole.SUPER_ADMIN;
    }
    return AdminUserEntity.AdminRole.ADMIN;
  }
}
