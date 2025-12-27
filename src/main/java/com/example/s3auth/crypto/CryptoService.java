package com.example.s3auth.crypto;

public interface CryptoService {
    /**
     * Encrypt plaintext bytes and return ciphertext blob (including any IV/metadata).
     */
    byte[] encrypt(byte[] plain) throws Exception;

    /**
     * Decrypt ciphertext bytes that were produced by encrypt.
     */
    byte[] decrypt(byte[] cipher) throws Exception;
}