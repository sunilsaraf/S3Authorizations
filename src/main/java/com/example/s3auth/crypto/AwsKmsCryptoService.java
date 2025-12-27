package com.example.s3auth.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptRequest;

import javax.annotation.PostConstruct;

/**
 * AWS KMS backed crypto service (envelope encryption has to be implemented in production: use KMS to encrypt a data key, then use data key locally).
 * This implementation demonstrates direct encrypt/decrypt via KMS (convenient but not ideal for large volumes).
 *
 * In production prefer: Generate a data key (GenerateDataKey), use it to encrypt locally, store encrypted data key + ciphertext.
 */
@Component
public class AwsKmsCryptoService implements CryptoService {

    @Value("${app.encryption.kms.key-arn:}")
    private String kmsKeyArn;

    private KmsClient kmsClient;

    @PostConstruct
    public void init() {
        if (kmsKeyArn == null || kmsKeyArn.isBlank()) {
            throw new IllegalStateException("KMS key ARN is required when KMS is enabled");
        }
        kmsClient = KmsClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Override
    public byte[] encrypt(byte[] plain) throws Exception {
        EncryptRequest req = EncryptRequest.builder()
                .keyId(kmsKeyArn)
                .plaintext(SdkBytes.fromByteArray(plain))
                .build();
        return kmsClient.encrypt(req).ciphertextBlob().asByteArray();
    }

    @Override
    public byte[] decrypt(byte[] cipher) throws Exception {
        DecryptRequest req = DecryptRequest.builder()
                .ciphertextBlob(SdkBytes.fromByteArray(cipher))
                .build();
        return kmsClient.decrypt(req).plaintext().asByteArray();
    }
}