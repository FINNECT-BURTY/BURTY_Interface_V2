package com.nuri.application.service;

import com.nuri.application.port.in.MyDataAuthUseCase;
import com.nuri.application.port.out.MyDataOAuthPort;
import org.springframework.stereotype.Service;

@Service
public class MyDataAuthService implements MyDataAuthUseCase {
    private final MyDataOAuthPort myDataOAuthPort;

    public MyDataAuthService(MyDataOAuthPort myDataOAuthPort) {
        this.myDataOAuthPort = myDataOAuthPort;
    }

    @Override
    public String createAuthorizeUrl(String userId) {
        return myDataOAuthPort.buildAuthorizeUrl(userId);
    }

    @Override
    public boolean exchangeAuthorizationCode(String userId, String code) {
        String accessToken = myDataOAuthPort.exchangeCodeForAccessToken(userId, code);
        return accessToken != null && !accessToken.isBlank();
    }
}
