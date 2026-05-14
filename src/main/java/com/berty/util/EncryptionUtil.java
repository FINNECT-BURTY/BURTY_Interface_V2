package com.berty.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-GCM 문자열 암복호화(기능명세 AES-GCM 방향 정합).
 * 레거시 AES/ECB(Base64) 형식은 {@link #decrypt}에서만 호환합니다.
 */
@Component
@Slf4j
public class EncryptionUtil {

    private static final String AES = "AES";
    private static final String GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String LEGACY_ECB_TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;
    private static final byte GCM_PAYLOAD_VERSION = 0x01;

    private final byte[] keyBytes;

    public EncryptionUtil(@Value("${app.encryption.secret-key:storyg-secret-key-32-characters!!}") String secretKey) {
        if (secretKey.length() < 16) {
            throw new IllegalArgumentException("암호화 키는 최소 16자 이상이어야 합니다");
        }
        String normalized;
        if (secretKey.length() < 32) {
            normalized = secretKey + "0".repeat(32 - secretKey.length());
        } else {
            normalized = secretKey.substring(0, 32);
        }
        this.keyBytes = normalized.getBytes(StandardCharsets.UTF_8);
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(GCM_TRANSFORMATION);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, AES);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[1 + IV_LENGTH + cipherText.length];
            combined[0] = GCM_PAYLOAD_VERSION;
            System.arraycopy(iv, 0, combined, 1, IV_LENGTH);
            System.arraycopy(cipherText, 0, combined, 1 + IV_LENGTH, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("암호화 실패", e);
            throw new RuntimeException("암호화 중 오류가 발생했습니다", e);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            if (decoded.length > 1 + IV_LENGTH && decoded[0] == GCM_PAYLOAD_VERSION) {
                try {
                    return decryptGcmPayload(decoded);
                } catch (Exception gcmEx) {
                    log.debug("GCM 복호화 실패, 레거시 ECB 시도: {}", gcmEx.getMessage());
                    return decryptLegacyEcb(encryptedText);
                }
            }
            return decryptLegacyEcb(encryptedText);
        } catch (Exception e) {
            log.error("복호화 실패: {}", encryptedText, e);
            throw new RuntimeException("복호화 중 오류가 발생했습니다", e);
        }
    }

    private String decryptGcmPayload(byte[] decoded) throws Exception {
        byte[] iv = Arrays.copyOfRange(decoded, 1, 1 + IV_LENGTH);
        byte[] cipherBytes = Arrays.copyOfRange(decoded, 1 + IV_LENGTH, decoded.length);
        Cipher cipher = Cipher.getInstance(GCM_TRANSFORMATION);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, AES);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] decrypted = cipher.doFinal(cipherBytes);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private String decryptLegacyEcb(String encryptedText) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, AES);
        Cipher cipher = Cipher.getInstance(LEGACY_ECB_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
