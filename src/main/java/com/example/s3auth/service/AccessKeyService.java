package com.example.s3auth.service;

import com.example.s3auth.crypto.CryptoService;
import com.example.s3auth.model.AccessKey;
import com.example.s3auth.repository.AccessKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class AccessKeyService {

    private final AccessKeyRepository repository;
    private final CryptoService cryptoService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.encryption.kms.enabled:false}")
    private boolean kmsEnabled;

    @Autowired
    public AccessKeyService(AccessKeyRepository repository, CryptoService cryptoService) {
        this.repository = repository;
        this.cryptoService = cryptoService;
    }

    /**
     * Create an access key and return (accessKeyId, secretAccessKey). Secret is only returned on creation.
     */
    public CreatedKey create(String userId, String description) throws Exception {
        String accessKeyId = generateAccessKeyId();
        String secret = generateSecret();

        byte[] secretBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] cipher = cryptoService.encrypt(secretBytes);

        String encMetadata = kmsEnabled ? "kms" : "local-aes";
        AccessKey ak = new AccessKey(accessKeyId, userId, cipher, encMetadata, description, Instant.now(), "ACTIVE");
        repository.save(ak);

        return new CreatedKey(accessKeyId, secret);
    }

    public Optional<AccessKey> get(String accessKeyId) {
        return repository.findById(accessKeyId);
    }

    public List<AccessKey> listByUser(String userId) {
        return repository.findByUserId(userId);
    }

    public void revoke(String accessKeyId) {
        repository.findById(accessKeyId).ifPresent(ak -> {
            ak.setStatus("REVOKED");
            repository.save(ak);
        });
    }

    public byte[] decryptSecret(AccessKey accessKey) throws Exception {
        return cryptoService.decrypt(accessKey.getSecretCiphertext());
    }

    private String generateAccessKeyId() {
        // AWS access key id format historically starts with "AKIA" but do not reuse that prefix if you don't want to
        // We'll produce a 20-char uppercase alphanumeric ID starting with "AK".
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder("AK");
        for (int i = 0; i < 18; i++) {
            sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String generateSecret() {
        // 40 bytes base64-like secret (not exactly AWS format). This is secretAccessKey value returned to client.
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().withoutPadding().encodeToString(bytes);
    }

    public static class CreatedKey {
        public final String accessKeyId;
        public final String secret;

        public CreatedKey(String accessKeyId, String secret) {
            this.accessKeyId = accessKeyId;
            this.secret = secret;
        }
    }
}