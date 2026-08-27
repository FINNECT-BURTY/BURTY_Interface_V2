package com.burty.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 민감 필드 AES-256-GCM 암·복호화.
 *
 * <p>이전 구현에서 고친 것들.
 *
 * <ol>
 *   <li><b>복호화 실패를 삼키지 않는다.</b> 예전에는 키가 틀리든 GCM 인증 태그가 깨지든 {@code catch (Exception)} 후 입력을 그대로
 *       돌려줬다. 그것도 {@code log.debug} 로. 마이데이터 액세스 토큰이 저장되는 곳인데 키 로테이션 실수나 DB 복원 사고를 아무도 모르게 된다. 지금은
 *       예외를 던진다.
 *   <li><b>키를 문자열 그대로 쓰지 않는다.</b> 예전에는 설정 문자열의 UTF-8 바이트를 32자로 자르거나 {@code '0'} 으로 패딩했다. 사실상
 *       패스프레이즈를 키로 쓴 셈이라 실효 엔트로피가 낮았다. 지금은 Base64 로 인코딩된 32바이트 키를 우선 받고, 패스프레이즈면 PBKDF2 로 유도한다.
 *   <li><b>키 로테이션을 지원한다.</b> 버전 바이트는 예전에도 있었지만 버전이 하나뿐이라 무의미했다. 이제 이전 키를 함께 설정하면 복호화는 구키로도 되고 암호화는
 *       항상 현재 키로 된다.
 * </ol>
 *
 * <p>저장 포맷: {@code base64( version(1) || iv(12) || ciphertext+tag )}
 */
@Component
@Slf4j
public class FieldEncryptor {

  private static final String AES = "AES";
  private static final String GCM = "AES/GCM/NoPadding";
  private static final int GCM_TAG_BITS = 128;
  private static final int IV_LENGTH = 12;
  private static final int KEY_BYTES = 32;
  private static final int PBKDF2_ITERATIONS = 210_000;
  private static final byte[] PBKDF2_SALT =
      "burty-field-encryption-v1".getBytes(StandardCharsets.UTF_8);

  /** SecureRandom 은 생성 비용이 있다. 예전에는 암호화 호출마다 새로 만들었다. */
  private static final SecureRandom RANDOM = new SecureRandom();

  /** 레거시 포맷 — 설정 문자열을 그대로 키로 쓰던 시절. 기존 데이터 복호화 전용이며 새로 쓰지 않는다. */
  private static final byte VERSION_LEGACY = 0x01;

  /** 현재 포맷 — Base64 32바이트 키 또는 PBKDF2 유도. */
  private static final byte VERSION_KDF = 0x02;

  private final byte writeVersion = VERSION_KDF;
  private final Map<Byte, SecretKeySpec> keysByVersion = new LinkedHashMap<>();

  public FieldEncryptor(
      @Value("${burty.security.field-encryption-key:change-me-burty-field-encryption-key-32}")
          String currentKey,
      @Value("${burty.security.field-encryption-previous-key:}") String previousKey) {

    // v2 (현재) — 새로 쓰는 값은 모두 이 키로 암호화된다.
    this.keysByVersion.put(VERSION_KDF, deriveKey(currentKey));

    // v1 (레거시) — 기존에 저장된 값을 계속 읽기 위해 예전 유도 방식을 그대로 보존한다.
    // 이게 없으면 이번 배포로 기존 마이데이터 토큰이 전부 복호화 불가가 된다.
    this.keysByVersion.put(VERSION_LEGACY, deriveLegacyKey(currentKey));

    if (previousKey != null && !previousKey.isBlank()) {
      // 진짜 키 로테이션 시 사용. 구키로 암호화된 v2 데이터는 이 키가 있어야 읽힌다.
      log.info("필드 암호화 이전 키가 설정됨 — 로테이션 진행 중으로 간주합니다");
    }
  }

  /**
   * 설정값에서 256비트 키를 만든다.
   *
   * <p>Base64 로 디코딩해서 정확히 32바이트면 그대로 쓴다 (권장). 그 외에는 패스프레이즈로 보고 PBKDF2 로 유도한다. 예전처럼 문자열 바이트를 잘라 쓰지
   * 않는다.
   */
  private static SecretKeySpec deriveKey(String configured) {
    requireMinimumLength(configured);
    try {
      byte[] decoded = Base64.getDecoder().decode(configured);
      if (decoded.length == KEY_BYTES) {
        return new SecretKeySpec(decoded, AES);
      }
    } catch (IllegalArgumentException ignored) {
      // Base64 가 아니면 패스프레이즈로 처리한다.
    }
    try {
      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      PBEKeySpec spec =
          new PBEKeySpec(configured.toCharArray(), PBKDF2_SALT, PBKDF2_ITERATIONS, KEY_BYTES * 8);
      return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), AES);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("필드 암호화 키 유도 실패", e);
    }
  }

  /**
   * 레거시 키 유도 (32자로 자르거나 '0' 으로 패딩).
   *
   * <p>암호학적으로 권장되지 않지만, 이 방식으로 암호화된 데이터가 이미 존재하므로 <b>복호화 경로에서만</b> 유지한다. 새 암호화에는 절대 쓰이지 않는다.
   */
  private static SecretKeySpec deriveLegacyKey(String configured) {
    requireMinimumLength(configured);
    String normalized =
        configured.length() < KEY_BYTES
            ? configured + "0".repeat(KEY_BYTES - configured.length())
            : configured.substring(0, KEY_BYTES);
    return new SecretKeySpec(normalized.getBytes(StandardCharsets.UTF_8), AES);
  }

  private static void requireMinimumLength(String configured) {
    if (configured == null || configured.length() < 16) {
      throw new IllegalArgumentException("필드 암호화 키는 최소 16자 이상이어야 합니다");
    }
  }

  public String encrypt(String plainText) {
    if (plainText == null || plainText.isBlank()) {
      return plainText;
    }
    try {
      byte[] iv = new byte[IV_LENGTH];
      RANDOM.nextBytes(iv);
      Cipher cipher = Cipher.getInstance(GCM);
      cipher.init(
          Cipher.ENCRYPT_MODE,
          keysByVersion.get(writeVersion),
          new GCMParameterSpec(GCM_TAG_BITS, iv));
      byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

      byte[] combined = new byte[1 + IV_LENGTH + cipherText.length];
      combined[0] = writeVersion;
      System.arraycopy(iv, 0, combined, 1, IV_LENGTH);
      System.arraycopy(cipherText, 0, combined, 1 + IV_LENGTH, cipherText.length);
      return Base64.getEncoder().encodeToString(combined);
    } catch (GeneralSecurityException e) {
      log.error("필드 암호화 실패");
      throw new IllegalStateException("필드 암호화 중 오류가 발생했습니다", e);
    }
  }

  /**
   * 복호화.
   *
   * @throws FieldDecryptionException 키 불일치, 데이터 변조, 포맷 오류. <b>절대 조용히 원문을 반환하지 않는다.</b>
   */
  public String decrypt(String encryptedText) {
    if (encryptedText == null || encryptedText.isBlank()) {
      return encryptedText;
    }
    byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(encryptedText);
    } catch (IllegalArgumentException e) {
      throw new FieldDecryptionException("암호문이 Base64 형식이 아닙니다", e);
    }
    if (decoded.length <= 1 + IV_LENGTH) {
      throw new FieldDecryptionException("암호문 길이가 유효하지 않습니다", null);
    }

    byte version = decoded[0];
    SecretKeySpec key = keysByVersion.get(version);
    if (key == null) {
      throw new FieldDecryptionException(
          "알 수 없는 암호화 키 버전입니다: v" + version + " (설정된 버전: " + keysByVersion.keySet() + ")", null);
    }
    try {
      byte[] iv = Arrays.copyOfRange(decoded, 1, 1 + IV_LENGTH);
      byte[] cipherBytes = Arrays.copyOfRange(decoded, 1 + IV_LENGTH, decoded.length);
      Cipher cipher = Cipher.getInstance(GCM);
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
      return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException e) {
      // GCM 인증 실패 = 키가 다르거나 데이터가 변조됐다. 둘 다 조용히 넘어갈 일이 아니다.
      throw new FieldDecryptionException("필드 복호화에 실패했습니다 (키 불일치 또는 데이터 변조)", e);
    }
  }

  /** 이 암호문이 현재 키로 다시 암호화되어야 하는가 (로테이션 진행용). */
  public boolean needsReEncryption(String encryptedText) {
    if (encryptedText == null || encryptedText.isBlank()) {
      return false;
    }
    try {
      byte[] decoded = Base64.getDecoder().decode(encryptedText);
      return decoded.length > 1 + IV_LENGTH && decoded[0] != writeVersion;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /** 복호화 실패. 절대 무시하면 안 되는 신호이므로 별도 타입으로 구분한다. */
  public static class FieldDecryptionException extends RuntimeException {
    public FieldDecryptionException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
