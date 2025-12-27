package com.example.s3auth.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateAccessKeyRequest {
    @NotBlank
    private String userId;

    private String description;

    public CreateAccessKeyRequest() {}

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
}