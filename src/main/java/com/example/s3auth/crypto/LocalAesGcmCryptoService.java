package com.example.s3auth.crypto;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Local AES-GCM implementation used as fallback when KMS is not enabled.
 * The master key must be supplied via environment variable APP_MASTER_KEY (base64-encoded 32 bytes).
 */
@Component
public class LocalAesGcmCryptoService implements CryptoService {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private final SecureRandom random = new SecureRandom();

    @Value("${APP_MASTER_KEY:}")
    private String base64MasterKey;

    private SecretKeySpec keySpec;

    @PostConstruct
    public void init() {
        if (base64MasterKey == null || base64MasterKey.isBlank()) {
            throw new IllegalStateException("APP_MASTER_KEY must be set to a base64-encoded 32-byte key when KMS is disabled");
        }
        byte[] key = Base64.getDecoder().decode(base64MasterKey);
        if (key.length != 32) {
            throw new IllegalStateException("APP_MASTER_KEY must be 32 bytes (base64-encoded)");
        }
        keySpec = new SecretKeySpec(key, "AES");
    }

    @Override
    public byte[] encrypt(byte[] plain) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        random.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);
        byte[] ct = cipher.doFinal(plain);

        // store iv + ct
        byte[] out = new byte[iv.length + ct.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(ct, 0, out, iv.length, ct.length);
        return out;
    }

    @Override
    public byte[] decrypt(byte[] cipherText) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(cipherText, 0, iv, 0, iv.length);
        int ctLength = cipherText.length - iv.length;
        byte[] ct = new byte[ctLength];
        System.arraycopy(cipherText, iv.length, ct, 0, ctLength);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
        return cipher.doFinal(ct);
    }
}