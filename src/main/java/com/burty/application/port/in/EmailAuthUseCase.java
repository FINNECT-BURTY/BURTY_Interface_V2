package com.burty.application.port.in;

import com.burty.domain.model.SocialLoginResult;

public interface EmailAuthUseCase {

    SocialLoginResult register(String email, String password);

    SocialLoginResult login(String email, String password);
}
