package com.berty.adapter.out.security;

import com.berty.application.port.out.KmsPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * AWS KMS adapter skeleton.
 *
 * To activate:
 *   1) Add software.amazon.awssdk:kms to build.gradle
 *   2) Set berty.kms.provider=AWS_KMS
 *   3) Configure AWS credentials (env / instance profile / SSO)
 *   4) Map aliases to KMS key IDs via berty.kms.aws.alias-map.* properties:
 *        berty.kms.aws.alias-map.berty.account-no=arn:aws:kms:ap-northeast-2:...:key/...
 *
 * Encrypt/decrypt should call KmsClient.encrypt/decrypt. Until the SDK is wired,
 * this skeleton fails fast so operators notice missing wiring instead of silently
 * passing through unencrypted bytes.
 */
@Component
@Primary
@ConditionalOnProperty(prefix = "berty.kms", name = "provider", havingValue = "AWS_KMS")
public class AwsKmsAdapter implements KmsPort {
    private static final Logger log = LoggerFactory.getLogger(AwsKmsAdapter.class);

    private final Map<String, String> aliasMap = new HashMap<>();
    private final String defaultRegion;

    public AwsKmsAdapter(@Value("${berty.kms.aws.region:ap-northeast-2}") String defaultRegion) {
        this.defaultRegion = defaultRegion;
        log.warn("AwsKmsAdapter is a skeleton — KMS SDK not wired. Add software.amazon.awssdk:kms and " +
                "implement encrypt/decrypt before going to production. region={}", defaultRegion);
    }

    @Override
    public String encrypt(String keyAlias, String plaintext) {
        throw new UnsupportedOperationException(
                "AWS KMS not wired. Add software.amazon.awssdk:kms to build.gradle, then implement encrypt/decrypt. alias=" + keyAlias);
    }

    @Override
    public String decrypt(String keyAlias, String ciphertext) {
        throw new UnsupportedOperationException(
                "AWS KMS not wired. Add software.amazon.awssdk:kms to build.gradle, then implement encrypt/decrypt. alias=" + keyAlias);
    }

    @Override
    public String providerId() {
        return "AWS_KMS";
    }

    /**
     * @return the resolved KMS key id for an alias, or the alias itself if no mapping exists.
     */
    String resolveKeyId(String alias) {
        return aliasMap.getOrDefault(alias, alias);
    }

    String region() {
        return defaultRegion;
    }
}