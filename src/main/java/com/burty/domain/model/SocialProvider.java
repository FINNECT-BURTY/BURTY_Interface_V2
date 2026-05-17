package com.burty.domain.model;

import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;

public enum SocialProvider {
    KAKAO, GOOGLE, NAVER, APPLE;

    public static SocialProvider parse(String raw) {
        if (raw == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "provider가 필요합니다.");
        }
        try {
            return SocialProvider.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "provider는 GOOGLE, KAKAO, NAVER, APPLE 중 하나여야 합니다.");
        }
    }
}
