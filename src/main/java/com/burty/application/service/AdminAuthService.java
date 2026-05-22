package com.burty.application.service;

import com.burty.application.port.in.AdminAuthUseCase;
import com.burty.config.AdminProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.entity.AdminUserEntity;
import com.burty.domain.repository.AdminUserRepository;
import com.burty.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuthService implements AdminAuthUseCase {

    private final AdminUserRepository adminUserRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    public AdminAuthService(AdminUserRepository adminUserRepository,
                            JwtTokenProvider jwtTokenProvider,
                            PasswordEncoder passwordEncoder,
                            AdminProperties adminProperties) {
        this.adminUserRepository = adminUserRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
    }

    @Override
    @Transactional(readOnly = true)
    public String login(String username, String password) {
        AdminUserEntity admin = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."));

        if (admin.getStatus() == AdminUserEntity.AdminStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "정지된 관리자 계정입니다.");
        }

        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        return jwtTokenProvider.generateToken(
                String.valueOf(admin.getAdminId()),
                "ROLE_ADMIN"
        );
    }

    @Override
    @Transactional
    public void register(String setupKey, String username, String password, String roleName) {
        String configuredKey = adminProperties.getSetupKey();
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "관리자 등록이 비활성화되어 있습니다. (burty.admin.setup-key 미설정)");
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
