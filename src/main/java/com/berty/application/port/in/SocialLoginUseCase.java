package com.berty.application.port.in;

import com.berty.domain.model.SocialAuthorizeUrlResult;
import com.berty.domain.model.SocialLoginResult;

public interface SocialLoginUseCase {
    SocialAuthorizeUrlResult createAuthorizeUrl(String provider, String state);

    SocialLoginResult login(String provider, String code, String redirectUri, String state, String codeVerifier);
}
