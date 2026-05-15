package com.burty.util;

import com.burty.application.port.out.KmsPort;
import org.springframework.stereotype.Component;

/**
 * Combines hashing, encryption, and masking for account numbers.
 * - hash: SHA-256, used for indexed lookup
 * - encrypt/decrypt: KmsPort (Local/AWS/Azure/GCP) — swap via burty.kms.provider
 * - mask: display-friendly form
 */
@Component
public class AccountNumberCipher {

    private static final String KEY_ALIAS = "burty.account-no";

    private final AccountNumberHasher hasher;
    private final KmsPort kmsPort;

    public AccountNumberCipher(AccountNumberHasher hasher, KmsPort kmsPort) {
        this.hasher = hasher;
        this.kmsPort = kmsPort;
    }

    public Encoded encode(String accountNo) {
        return new Encoded(
                hasher.hash(accountNo),
                kmsPort.encrypt(KEY_ALIAS, accountNo),
                hasher.mask(accountNo)
        );
    }

    public String decrypt(String encrypted) {
        return kmsPort.decrypt(KEY_ALIAS, encrypted);
    }

    public String hash(String accountNo) {
        return hasher.hash(accountNo);
    }

    public String mask(String accountNo) {
        return hasher.mask(accountNo);
    }

    public record Encoded(String hash, String encrypted, String masked) {}
}
