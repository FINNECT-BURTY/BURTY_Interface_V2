package com.burty.application.port.out;

import java.time.LocalDateTime;

public interface MyDataOAuthPort {
    String buildAuthorizeUrl(String userId);
    String exchangeCodeForAccessToken(String userId, String code);
    String findAccessToken(String userId);
    String refreshAccessToken(String userId);

    /** Returns the access token expiry as a LocalDateTime, or null if unknown. */
    default LocalDateTime findTokenExpiresAt(String userId) {
        return null;
    }
}
