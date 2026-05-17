package com.burty.application.port.in;

import com.burty.domain.model.SocialAuthorizeUrlResult;
import com.burty.domain.model.SocialLoginResult;

public interface SocialLoginUseCase {
    /**
     * @param redirectUri (선택) FE 가 자기 콜백 페이지 URL 을 명시적으로 지정.
     *                    null/blank 이면 application-{profile}.properties 의 burty.social.<provider>.redirect-uri default 사용.
     *                    이 값은 OAuth provider 콘솔에 사전 등록된 redirect URI 중 하나여야 함.
     */
    SocialAuthorizeUrlResult createAuthorizeUrl(String provider, String state, String redirectUri);

    SocialLoginResult login(String provider, String code, String redirectUri, String state, String codeVerifier);
}
