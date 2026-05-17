package com.burty.adapter.out.social;

import com.burty.domain.model.SocialProfile;
import com.burty.domain.model.SocialProvider;

/**
 * Provider 별로 다른 OAuth 디테일(authorize URL 추가 파라미터, userinfo 응답 파싱,
 * Apple 처럼 id_token 직접 처리 등)을 캡슐화. 새 provider 추가 시 strategy 클래스 1개만 추가.
 */
public interface SocialProviderStrategy {
    SocialProvider supports();

    /** authorize URL 에 provider 별 추가 query param (e.g. Apple response_mode=form_post) 적용. */
    default void customizeAuthorizeUrl(org.springframework.web.util.UriComponentsBuilder builder) {
        // 기본은 추가 없음
    }

    /** authorization code → access/id token 교환 후 userinfo 까지 조회해 SocialProfile 반환. */
    SocialProfile fetchProfile(String code, String redirectUri, String codeVerifier);
}
