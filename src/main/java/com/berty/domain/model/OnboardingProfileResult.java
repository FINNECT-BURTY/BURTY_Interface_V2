package com.berty.domain.model;

/**
 * 소셜 로그인 이후 추가 프로필 등록 결과.
 */
public record OnboardingProfileResult(boolean completed, boolean alreadyRegistered) {
}
