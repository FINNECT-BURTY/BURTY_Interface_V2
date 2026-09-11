package com.burty.application.port.out.mydata;

import com.burty.domain.mydata.model.MyDataTokenBundle;
import java.time.LocalDateTime;

public interface MyDataOAuthPort {
  String buildAuthorizeUrl(String stateKey);

  MyDataTokenBundle exchangeTokens(String stateKey, String code);

  String findAccessToken(String stateKey);

  String findRefreshToken(String stateKey);

  String refreshAccessToken(String stateKey);

  /**
   * 정보제공자에 토큰 폐기를 요청한다.
   *
   * <p>로컬에서 지우는 것만으로는 부족하다. 토큰은 정보제공자 쪽에서도 유효하므로, 어딘가에 남아 있다면 우리가 지운 뒤에도 쓰일 수 있다.
   *
   * @throws RuntimeException 폐기를 확인하지 못했을 때. 호출부는 로컬 무효화를 먼저 끝낸 뒤 부른다.
   */
  void revokeGrant(String institutionCode, String token);

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
