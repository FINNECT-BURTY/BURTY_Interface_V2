package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.util.FieldEncryptor;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 필드 암호화. 중점은 <b>실패가 실패로 드러나는가</b> 와 <b>기존 데이터를 계속 읽을 수 있는가</b>. */
class FieldEncryptorTests {

  private static final String KEY = "unit-test-field-encryption-key-value";

  private FieldEncryptor encryptor() {
    return new FieldEncryptor(KEY, "");
  }

  @Test
  @DisplayName("암호화-복호화 왕복")
  void roundTrip() {
    FieldEncryptor enc = encryptor();
    String plain = "mydata-access-token-abc123";
    String cipher = enc.encrypt(plain);

    assertNotEquals(plain, cipher);
    assertEquals(plain, enc.decrypt(cipher));
  }

  @Test
  @DisplayName("같은 평문도 매번 다른 암호문이 된다 (IV 재사용 없음)")
  void encryptionIsNonDeterministic() {
    FieldEncryptor enc = encryptor();
    assertNotEquals(enc.encrypt("same"), enc.encrypt("same"));
  }

  @Test
  @DisplayName("키가 다르면 복호화가 조용히 성공하지 않고 예외를 던진다")
  void wrongKeyThrowsInsteadOfReturningGarbage() {
    String cipher = encryptor().encrypt("secret-token");
    FieldEncryptor other = new FieldEncryptor("completely-different-key-material", "");

    assertThrows(FieldEncryptor.FieldDecryptionException.class, () -> other.decrypt(cipher));
  }

  @Test
  @DisplayName("변조된 암호문은 GCM 인증 실패로 거부된다")
  void tamperedCiphertextIsRejected() {
    FieldEncryptor enc = encryptor();
    byte[] raw = Base64.getDecoder().decode(enc.encrypt("secret-token"));
    raw[raw.length - 1] ^= 0x01; // 태그 1비트 변조
    String tampered = Base64.getEncoder().encodeToString(raw);

    assertThrows(FieldEncryptor.FieldDecryptionException.class, () -> enc.decrypt(tampered));
  }

  @Test
  @DisplayName("Base64 가 아닌 입력을 그대로 돌려주지 않는다")
  void nonBase64InputIsRejected() {
    assertThrows(
        FieldEncryptor.FieldDecryptionException.class, () -> encryptor().decrypt("not-base64!!!"));
  }

  @Test
  @DisplayName("레거시(v1) 방식으로 암호화된 기존 데이터를 계속 읽을 수 있다")
  void legacyCiphertextRemainsReadable() throws Exception {
    String plain = "legacy-stored-token";
    String legacyCipher = encryptLegacy(plain);

    FieldEncryptor enc = encryptor();
    assertEquals(plain, enc.decrypt(legacyCipher), "기존 암호문을 못 읽으면 배포 즉시 마이데이터 연동이 끊긴다");
    assertTrue(enc.needsReEncryption(legacyCipher), "레거시 암호문은 재암호화 대상으로 표시되어야 한다");
    assertFalse(enc.needsReEncryption(enc.encrypt(plain)), "현재 키로 쓴 값은 재암호화 대상이 아니다");
  }

  @Test
  @DisplayName("빈 값과 null 은 그대로 통과한다")
  void blankValuesPassThrough() {
    FieldEncryptor enc = encryptor();
    assertEquals(null, enc.encrypt(null));
    assertEquals("", enc.decrypt(""));
  }

  /** 예전 구현과 동일한 방식으로 암호문을 만든다 (키를 잘라 쓰고 version 바이트 0x01). */
  private static String encryptLegacy(String plain) throws Exception {
    String normalized =
        KEY.length() < 32 ? KEY + "0".repeat(32 - KEY.length()) : KEY.substring(0, 32);
    SecretKeySpec key = new SecretKeySpec(normalized.getBytes(StandardCharsets.UTF_8), "AES");
    byte[] iv = new byte[12];
    new SecureRandom().nextBytes(iv);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
    byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

    byte[] combined = new byte[1 + 12 + ct.length];
    combined[0] = 0x01;
    System.arraycopy(iv, 0, combined, 1, 12);
    System.arraycopy(ct, 0, combined, 13, ct.length);
    return Base64.getEncoder().encodeToString(combined);
  }
}
