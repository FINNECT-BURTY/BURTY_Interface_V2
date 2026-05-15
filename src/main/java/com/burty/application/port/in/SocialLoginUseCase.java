package com.burty.application.port.in;

import com.burty.domain.model.SocialAuthorizeUrlResult;
import com.burty.domain.model.SocialLoginResult;

public interface SocialLoginUseCase {
    SocialAuthorizeUrlResult createAuthorizeUrl(String provider, String state);

    SocialLoginResult login(String provider, String code, String redirectUri, String state, String codeVerifier);
}
