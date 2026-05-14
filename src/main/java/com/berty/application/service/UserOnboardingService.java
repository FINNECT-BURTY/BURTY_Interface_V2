package com.berty.application.service;

import com.berty.application.port.in.UserOnboardingUseCase;
import com.berty.core.error.enums.ErrorCode;
import com.berty.core.exception.BusinessException;
import com.berty.config.BertyOnboardingProperties;
import com.berty.domain.entity.ConsentRecordEntity;
import com.berty.domain.entity.UserEntity;
import com.berty.domain.entity.UserProfileEntity;
import com.berty.domain.model.OnboardingProfileResult;
import com.berty.domain.repository.ConsentRecordRepository;
import com.berty.domain.repository.UserProfileRepository;
import com.berty.domain.repository.UserRepository;
import com.berty.util.EncryptionUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.UUID;

@Service
public class UserOnboardingService implements UserOnboardingUseCase {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ConsentRecordRepository consentRecordRepository;
    private final EncryptionUtil encryptionUtil;
    private final BertyOnboardingProperties onboardingProperties;

    public UserOnboardingService(UserRepository userRepository,
                                 UserProfileRepository userProfileRepository,
                                 ConsentRecordRepository consentRecordRepository,
                                 EncryptionUtil encryptionUtil,
                                 BertyOnboardingProperties onboardingProperties) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.consentRecordRepository = consentRecordRepository;
        this.encryptionUtil = encryptionUtil;
        this.onboardingProperties = onboardingProperties;
    }

    @Override
    @Transactional
    public OnboardingProfileResult completeProfile(
            String userId,
            String phone,
            String name,
            LocalDate birthDate,
            Integer ageRange,
            String uxModeRaw,
            boolean termsAccepted) {
        if (!termsAccepted) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "필수 약관(LGN-006)에 동의해야 합니다.");
        }
        if (blank(name)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "실명을 입력해 주세요.");
        }
        if (birthDate == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "생년월일을 입력해 주세요.");
        }
        if (birthDate.isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "생년월일이 올바르지 않습니다.");
        }
        if (Period.between(birthDate, LocalDate.now()).getYears() > 120) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "생년월일이 올바르지 않습니다.");
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "userId 형식이 올바르지 않습니다.");
        }

        if (userProfileRepository.existsById(uuid)) {
            return new OnboardingProfileResult(true, true);
        }

        String normalizedPhone = normalizeKoreanMobile(phone);
        String phoneHash = sha256(normalizedPhone);
        userRepository.findByPhoneHash(phoneHash)
                .filter(u -> !u.getUserId().equals(uuid))
                .ifPresent(u -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "이미 사용 중인 휴대폰 번호입니다.");
                });

        UserEntity user = userRepository.findById(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        user.setPhoneHash(phoneHash);
        user.setPhoneEncrypted(encryptionUtil.encrypt(normalizedPhone).getBytes(StandardCharsets.UTF_8));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        UserProfileEntity profile = new UserProfileEntity();
        profile.setUser(user);
        profile.setNameEncrypted(encryptionUtil.encrypt(name.trim()).getBytes(StandardCharsets.UTF_8));
        profile.setBirthdateEncrypted(encryptionUtil.encrypt(birthDate.toString()).getBytes(StandardCharsets.UTF_8));
        profile.setAgeRange(ageRange);
        profile.setUxMode(parseUxMode(uxModeRaw));
        profile.setFontScale(BigDecimal.ONE);
        profile.setVoiceEnabled(false);
        LocalDateTime now = LocalDateTime.now();
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        userProfileRepository.save(profile);

        persistSignupConsents(user, now);

        return new OnboardingProfileResult(true, false);
    }

    private void persistSignupConsents(UserEntity user, LocalDateTime agreedAt) {
        persistConsent(user, ConsentRecordEntity.ConsentType.TERMS, onboardingProperties.getTermsVersion(), agreedAt);
        persistConsent(user, ConsentRecordEntity.ConsentType.PRIVACY, onboardingProperties.getPrivacyVersion(), agreedAt);
    }

    private void persistConsent(UserEntity user, ConsentRecordEntity.ConsentType type, String version, LocalDateTime agreedAt) {
        ConsentRecordEntity c = new ConsentRecordEntity();
        c.setConsentId(UUID.randomUUID());
        c.setUser(user);
        c.setConsentType(type);
        c.setConsentVersion(version);
        c.setDocumentHash(sha256(type.name() + "|v=" + version));
        c.setAgreedAt(agreedAt);
        consentRecordRepository.save(c);
    }

    private UserProfileEntity.UxMode parseUxMode(String raw) {
        if (blank(raw)) {
            return UserProfileEntity.UxMode.STANDARD;
        }
        try {
            return UserProfileEntity.UxMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "uxMode는 SENIOR 또는 STANDARD 여야 합니다.");
        }
    }

    private String normalizeKoreanMobile(String raw) {
        if (blank(raw)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "휴대폰 번호를 입력해 주세요.");
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.startsWith("82")) {
            digits = "0" + digits.substring(2);
        }
        if (digits.length() < 10 || digits.length() > 11 || !digits.startsWith("0")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "휴대폰 번호 형식이 올바르지 않습니다.");
        }
        return digits;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 hash failed", e);
        }
    }
}
