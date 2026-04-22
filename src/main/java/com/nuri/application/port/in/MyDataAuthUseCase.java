package com.nuri.application.port.in;

public interface MyDataAuthUseCase {
    String createAuthorizeUrl(String userId);
    boolean exchangeAuthorizationCode(String userId, String code);
}
