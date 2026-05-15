package com.burty.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Account number SHA-256 hashing for indexed lookup without storing raw PII.
 * Pair with EncryptionUtil to keep encrypted form for retrieval and masked form for display.
 */
@Component
public class AccountNumberHasher {

    public String hash(String accountNo) {
        if (accountNo == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(accountNo.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public String mask(String accountNo) {
        if (accountNo == null || accountNo.length() < 4) return "****";
        int visibleTail = Math.min(4, accountNo.length() - 4);
        int hidden = accountNo.length() - visibleTail;
        return "*".repeat(Math.max(4, hidden)) + accountNo.substring(accountNo.length() - visibleTail);
    }
}
