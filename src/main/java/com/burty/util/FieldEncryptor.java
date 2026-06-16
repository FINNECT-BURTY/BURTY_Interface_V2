package com.burty.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 민감 필드 AES-GCM 암·복호화. */
@Component
@Slf4j
public class FieldEncryptor {

  private static final String AES = "AES";
  private static final String GCM = "AES/GCM/NoPadding";
  private static final int GCM_TAG_BITS = 128;
  private static final int IV_LENGTH = 12;
  private static final byte VERSION = 0x01;

  private final byte[] keyBytes;

  public FieldEncryptor(
      @Value("${burty.security.field-encryption-key:change-me-burty-field-encryption-key-32}")
          String secretKey) {
    if (secretKey.length() < 16) {
      throw new IllegalArgumentException("필드 암호화 키는 최소 16자 이상이어야 합니다");
    }
    String normalized =
        secretKey.length() < 32
            ? secretKey + "0".repeat(32 - secretKey.length())
            : secretKey.substring(0, 32);
    this.keyBytes = normalized.getBytes(StandardCharsets.UTF_8);
  }

  public String encrypt(String plainText) {
    if (plainText == null || plainText.isBlank()) {
      return plainText;
    }
    try {
      byte[] iv = new byte[IV_LENGTH];
      new SecureRandom().nextBytes(iv);
      Cipher cipher = Cipher.getInstance(GCM);
      cipher.init(
          Cipher.ENCRYPT_MODE,
          new SecretKeySpec(keyBytes, AES),
          new GCMParameterSpec(GCM_TAG_BITS, iv));
      byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
      byte[] combined = new byte[1 + IV_LENGTH + cipherText.length];
      combined[0] = VERSION;
      System.arraycopy(iv, 0, combined, 1, IV_LENGTH);
      System.arraycopy(cipherText, 0, combined, 1 + IV_LENGTH, cipherText.length);
      return Base64.getEncoder().encodeToString(combined);
    } catch (Exception e) {
      log.error("필드 암호화 실패", e);
      throw new IllegalStateException("필드 암호화 중 오류가 발생했습니다", e);
    }
  }

  public String decrypt(String encryptedText) {
    if (encryptedText == null || encryptedText.isBlank()) {
      return encryptedText;
    }
    try {
      byte[] decoded = Base64.getDecoder().decode(encryptedText);
      if (decoded.length <= 1 + IV_LENGTH || decoded[0] != VERSION) {
        return encryptedText;
      }
      byte[] iv = Arrays.copyOfRange(decoded, 1, 1 + IV_LENGTH);
      byte[] cipherBytes = Arrays.copyOfRange(decoded, 1 + IV_LENGTH, decoded.length);
      Cipher cipher = Cipher.getInstance(GCM);
      cipher.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(keyBytes, AES),
          new GCMParameterSpec(GCM_TAG_BITS, iv));
      return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
    } catch (Exception e) {
      log.debug("필드 복호화 실패(평문 후보): {}", e.getMessage());
      return encryptedText;
    }
  }
}
