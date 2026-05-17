package com.burty.domain.model;

/**
 * OAuth provider 가 반환한 사용자 식별 정보. providerUserId 만 필수,
 * email/displayName 은 동의/검수 여부에 따라 null 일 수 있음.
 */
public record SocialProfile(String providerUserId, String email, String displayName) {}
