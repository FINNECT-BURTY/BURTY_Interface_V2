package com.burty.application.port.out.mydata;

import com.burty.domain.mydata.model.MyDataTokenBundle;
import java.time.LocalDateTime;

public interface MyDataOAuthPort {
  String buildAuthorizeUrl(String stateKey);

  /**
   * @deprecated {@link #exchangeTokens(String, String)} 사용
   */
  default String exchangeCodeForAccessToken(String stateKey, String code) {
    MyDataTokenBundle bundle = exchangeTokens(stateKey, code);
    return bundle == null ? null : bundle.accessToken();
  }

  MyDataTokenBundle exchangeTokens(String stateKey, String code);

  String findAccessToken(String stateKey);

  String findRefreshToken(String stateKey);

  String refreshAccessToken(String stateKey);

  default LocalDateTime findTokenExpiresAt(String stateKey) {
    return null;
  }

  /** userId 와 institutionCode 로 TokenStore 키 생성. */
  static String scopeKey(String userId, String institutionCode) {
    if (institutionCode == null || institutionCode.isBlank() || "MYDATA".equals(institutionCode)) {
      return userId;
    }
    return userId + "::" + institutionCode;
  }

  static String parseUserId(String stateKey) {
    int idx = stateKey.indexOf("::");
    return idx < 0 ? stateKey : stateKey.substring(0, idx);
  }

  static String parseInstitutionCode(String stateKey) {
    int idx = stateKey.indexOf("::");
    return idx < 0 ? "MYDATA" : stateKey.substring(idx + 2);
  }
}
