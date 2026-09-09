package com.burty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.burty.adapter.out.security.WebAuthnCredentialStore;
import com.burty.domain.auth.entity.BiometricCredentialEntity;
import com.burty.domain.auth.repository.BiometricCredentialRepository;
import com.burty.domain.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 어설션 검증이 서버 시크릿 없이 통과하지 않는지 확인한다.
 *
 * <p>{@code WebAuthnSignature.sign} 이 예외를 삼키고 빈 문자열을 돌려주고 있었다. 호출부는 그 값을 그대로 비교하므로, 서버 시크릿이 비면 기대값도
 * {@code ""} 가 되고 접두사만 있는 토큰의 서명도 {@code ""} 라 인증이 통과했다. <b>시크릿을 지우는 것이 인증을 끄는 결과</b>였다.
 *
 * <p>이 패키지는 커버리지 5.1% 에 참조 테스트가 하나도 없었는데, 인증 우회가 여기서 세 번 나왔다(#84, #119, #132). 이체를 지키는 마지막 관문이라 동작을
 * 못 박아 둔다.
 */
class WebAuthnAssertionBypassTests {

  private static final String USER_ID = "1";
  private static final String CREDENTIAL_ID = "cred-raw";
  private static final String SECRET = "unit-test-webauthn-secret";

  private WebAuthnCredentialStore storeWithCredential() {
    BiometricCredentialEntity credential = new BiometricCredentialEntity();
    credential.setCredentialIdRaw(CREDENTIAL_ID.getBytes(StandardCharsets.UTF_8));

    BiometricCredentialRepository credentials = mock(BiometricCredentialRepository.class);
    when(credentials.findFirstByUser_UserIdAndRevokedAtIsNull(anyLong()))
        .thenReturn(Optional.of(credential));

    return new WebAuthnCredentialStore(credentials, mock(UserRepository.class));
  }

  /** 서버가 실제로 만드는 것과 같은 방식의 유효한 토큰. */
  private static String validToken(String secret) throws Exception {
    Mac hmac = Mac.getInstance("HmacSHA256");
    hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    String signature =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                hmac.doFinal((USER_ID + ":" + CREDENTIAL_ID).getBytes(StandardCharsets.UTF_8)));
    return "webauthn:" + signature;
  }

  @Test
  @DisplayName("서버 시크릿이 비어 있으면 접두사만 있는 토큰으로 통과할 수 없다")
  void emptySecretDoesNotAuthenticate() {
    // 예전에는 여기서 통과했다. sign 이 "" 를 돌려주고 공격자 서명도 "" 라 같아졌다.
    // 배포 템플릿에서 WEBAUTHN_SERVER_SECRET 를 선언만 하고 비워두면 실제로 도달한다.
    assertFalse(storeWithCredential().verifyAssertion(USER_ID, "webauthn:", ""));
  }

  @Test
  @DisplayName("서버 시크릿이 null 이어도 통과할 수 없다")
  void nullSecretDoesNotAuthenticate() {
    assertFalse(storeWithCredential().verifyAssertion(USER_ID, "webauthn:", null));
  }

  @Test
  @DisplayName("시크릿이 비면 유효한 서명을 가져와도 통과할 수 없다")
  void emptySecretRejectsEvenRealSignature() throws Exception {
    // 서명 자체는 올바른데 서버가 검증할 수 없는 상태다. 이때 통과시키면 검증이
    // 사실상 없는 것과 같다.
    String token = validToken(SECRET);
    assertFalse(storeWithCredential().verifyAssertion(USER_ID, token, ""));
  }

  @Test
  @DisplayName("정상 시크릿과 올바른 서명은 통과한다")
  void validSignaturePasses() throws Exception {
    // 위 거부들이 "전부 거부" 로 통과하는 가짜 테스트가 아님을 보인다.
    assertTrue(storeWithCredential().verifyAssertion(USER_ID, validToken(SECRET), SECRET));
  }

  @Test
  @DisplayName("다른 시크릿으로 만든 서명은 거부한다")
  void signatureFromOtherSecretRejected() throws Exception {
    assertFalse(storeWithCredential().verifyAssertion(USER_ID, validToken("other-secret"), SECRET));
  }

  @Test
  @DisplayName("접두사가 없는 토큰은 거부한다")
  void tokenWithoutPrefixRejected() {
    assertFalse(storeWithCredential().verifyAssertion(USER_ID, "deadbeef", SECRET));
  }

  @Test
  @DisplayName("숫자가 아닌 userId 는 거부한다")
  void nonNumericUserIdRejected() throws Exception {
    // 데모 세션(demo-user)이 여기 걸린다. #119 와 같은 자리다.
    assertFalse(storeWithCredential().verifyAssertion("demo-user", validToken(SECRET), SECRET));
  }
}
