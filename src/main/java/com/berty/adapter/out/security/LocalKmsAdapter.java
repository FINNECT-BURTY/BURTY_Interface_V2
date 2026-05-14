package com.berty.adapter.out.security;

import com.berty.application.port.out.KmsPort;
import com.berty.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Local KMS adapter — wraps the existing AES-based EncryptionUtil.
 * Active when berty.kms.provider=LOCAL (default).
 *
 * NOTE: Plaintext key lives in application.properties / env. Acceptable for dev/sandbox,
 * NOT for production. For production, switch berty.kms.provider=AWS|AZURE|GCP and remove
 * the local secret from configs.
 */
@Component
@Primary
@ConditionalOnProperty(prefix = "berty.kms", name = "provider", havingValue = "LOCAL", matchIfMissing = true)
public class LocalKmsAdapter implements KmsPort {

    private final EncryptionUtil encryptionUtil;
    private final boolean strictAliasCheck;

    public LocalKmsAdapter(EncryptionUtil encryptionUtil,
                           @Value("${berty.kms.local.strict-alias-check:false}") boolean strictAliasCheck) {
        this.encryptionUtil = encryptionUtil;
        this.strictAliasCheck = strictAliasCheck;
    }

    @Override
    public String encrypt(String keyAlias, String plaintext) {
        validateAlias(keyAlias);
        return encryptionUtil.encrypt(plaintext);
    }

    @Override
    public String decrypt(String keyAlias, String ciphertext) {
        validateAlias(keyAlias);
        return encryptionUtil.decrypt(ciphertext);
    }

    @Override
    public String providerId() {
        return "LOCAL";
    }

    private void validateAlias(String keyAlias) {
        if (strictAliasCheck && (keyAlias == null || keyAlias.isBlank())) {
            throw new IllegalArgumentException("keyAlias must not be empty");
        }
    }
}