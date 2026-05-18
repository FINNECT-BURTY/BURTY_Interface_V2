package com.burty.application.port.in;

import com.burty.domain.model.SocialAuthorizeUrlResult;
import com.burty.domain.model.SocialLoginResult;

public interface SocialLoginUseCase {
    /**
     * @param frontendOrigin 로그인 완료 후 돌려보낼 프론트 origin. null/blank이면 서버 기본 FRONTEND_URL 사용.
     */
    SocialAuthorizeUrlResult createAuthorizeUrl(String provider, String state, String frontendOrigin);

    SocialLoginResult login(String provider, String code, String redirectUri, String state, String codeVerifier);
}
