package com.example.s3auth.model;

import org.springframework.data.annotation.Transient;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.*;

import java.time.Instant;
import java.util.UUID;

@Table(value = "access_keys")
public class AccessKey {

    @PrimaryKey
    private String accessKeyId;

    @Column("user_id")
    private String userId;

    /**
     * Encrypted secret blob (ciphertext).
     */
    @Column("secret_ciphertext")
    private byte[] secretCiphertext;

    @Column("encryption_metadata")
    private String encryptionMetadata;

    @Column("description")
    private String description;

    @Column("created_at")
    private Instant createdAt;

    @Column("status")
    private String status;

    public AccessKey() {}

    public AccessKey(String accessKeyId, String userId, byte[] secretCiphertext, String encryptionMetadata, String description, Instant createdAt, String status) {
        this.accessKeyId = accessKeyId;
        this.userId = userId;
        this.secretCiphertext = secretCiphertext;
        this.encryptionMetadata = encryptionMetadata;
        this.description = description;
        this.createdAt = createdAt;
        this.status = status;
    }

    // Getters and setters

    public String getAccessKeyId() {
        return accessKeyId;
    }
    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public byte[] getSecretCiphertext() {
        return secretCiphertext;
    }
    public void setSecretCiphertext(byte[] secretCiphertext) {
        this.secretCiphertext = secretCiphertext;
    }
    public String getEncryptionMetadata() {
        return encryptionMetadata;
    }
    public void setEncryptionMetadata(String encryptionMetadata) {
        this.encryptionMetadata = encryptionMetadata;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}