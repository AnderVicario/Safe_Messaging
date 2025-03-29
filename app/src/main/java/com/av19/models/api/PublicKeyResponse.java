package com.av19.models.api;

public class PublicKeyResponse {
    private String public_key;

    public PublicKeyResponse() { }

    public PublicKeyResponse(String public_key) {
        this.public_key = public_key;
    }

    public String getPublic_key() {
        return public_key;
    }

    public void setPublic_key(String public_key) {
        this.public_key = public_key;
    }
}