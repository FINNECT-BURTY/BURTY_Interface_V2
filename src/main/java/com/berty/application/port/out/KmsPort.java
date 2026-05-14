package com.berty.application.port.out;

/**
 * Key Management Service abstraction.
 *
 * Production deployments swap LocalKmsAdapter (AES key from properties) for AwsKmsAdapter,
 * AzureKeyVaultAdapter, or GoogleCloudKmsAdapter without touching callers.
 *
 * keyAlias selects the logical key (e.g., "berty.account-no", "berty.smtp-password").
 * Adapters resolve aliases to actual KMS key IDs internally.
 */
public interface KmsPort {

    String encrypt(String keyAlias, String plaintext);

    String decrypt(String keyAlias, String ciphertext);

    /** Identifies which provider is wired (LOCAL/AWS_KMS/AZURE/GCP). */
    String providerId();
}