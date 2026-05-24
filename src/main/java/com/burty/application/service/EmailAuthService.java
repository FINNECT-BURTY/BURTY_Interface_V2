package com.burty.application.service;

import com.burty.application.port.in.EmailAuthUseCase;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.entity.EmailAccountEntity;
import com.burty.domain.entity.UserEntity;
import com.burty.domain.model.SocialLoginResult;
import com.burty.domain.repository.EmailAccountRepository;
import com.burty.domain.repository.UserProfileRepository;
import com.burty.domain.repository.UserRepository;
import com.burty.security.RefreshTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
public class EmailAuthService implements EmailAuthUseCase {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    private final EmailAccountRepository emailAccountRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    public EmailAuthService(EmailAccountRepository emailAccountRepository,
                            UserRepository userRepository,
                            UserProfileRepository userProfileRepository,
                            RefreshTokenService refreshTokenService,
                            PasswordEncoder passwordEncoder) {
        this.emailAccountRepository = emailAccountRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public SocialLoginResult register(String email, String password) {
        validateEmail(email);
        validatePassword(password);

        String normalizedEmail = email.trim().toLowerCase();
        String emailHash = sha256(normalizedEmail);

        if (emailAccountRepository.existsByEmailHash(emailHash)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS, "이미 사용 중인 이메일입니다.");
        }

        UserEntity user = buildEmailUser(normalizedEmail);
        userRepository.save(user);

        EmailAccountEntity emailAccount = new EmailAccountEntity();
        emailAccount.setUserId(user.getUserId());
        emailAccount.setEmail(normalizedEmail);
        emailAccount.setEmailHash(emailHash);
        emailAccount.setPasswordHash(passwordEncoder.encode(password));
        emailAccountRepository.save(emailAccount);

        RefreshTokenService.TokenPair tokens =
                refreshTokenService.issueNewSession(user.getUserId().toString(), null);

        return new SocialLoginResult(
                user.getUserId().toString(),
                "EMAIL",
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.accessExpiresInSeconds(),
                tokens.refreshExpiresInSeconds(),
                true,
                false,
                null
        );
    }

    @Override
    @Transactional
    public SocialLoginResult login(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이메일을 입력해주세요.");
        }
        if (password == null || password.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "비밀번호를 입력해주세요.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        String emailHash = sha256(normalizedEmail);

        EmailAccountEntity emailAccount = emailAccountRepository.findByEmailHash(emailHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(password, emailAccount.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        UserEntity user = userRepository.findById(emailAccount.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (user.getStatus() != UserEntity.UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE, "비활성화된 계정입니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        emailAccount.setLastLoginAt(now);
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);

        boolean profileComplete = userProfileRepository.existsById(user.getUserId());
        RefreshTokenService.TokenPair tokens =
                refreshTokenService.issueNewSession(user.getUserId().toString(), null);

        return new SocialLoginResult(
                user.getUserId().toString(),
                "EMAIL",
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.accessExpiresInSeconds(),
                tokens.refreshExpiresInSeconds(),
                false,
                profileComplete,
                null
        );
    }

    private UserEntity buildEmailUser(String email) {
        LocalDateTime now = LocalDateTime.now();
        String seed = "EMAIL|" + email;
        UserEntity user = new UserEntity();
        user.setCiHash(sha256("EMAIL_CI|" + seed));
        user.setCi("EMAIL_CI|" + seed);
        user.setPhoneHash(sha256("EMAIL_PHONE|" + seed));
        user.setPhone("UNVERIFIED");
        user.setStatus(UserEntity.UserStatus.ACTIVE);
        user.setFailedLoginCount(0);
        user.setLastLoginAt(now);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return user;
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이메일을 입력해주세요.");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new BusinessException(ErrorCode.INVALID_EMAIL_FORMAT, "올바른 이메일 형식이 아닙니다.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "비밀번호를 입력해주세요.");
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_FORMAT,
                    "비밀번호는 영문자와 숫자를 포함하여 8자 이상이어야 합니다.");
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 hash failed", e);
        }
    }
}
