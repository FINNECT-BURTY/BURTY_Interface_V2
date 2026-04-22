package com.nuri.application.port.out;

public interface MyDataOAuthPort {
    String buildAuthorizeUrl(String userId);
    String exchangeCodeForAccessToken(String userId, String code);
    String findAccessToken(String userId);
    String refreshAccessToken(String userId);
}
