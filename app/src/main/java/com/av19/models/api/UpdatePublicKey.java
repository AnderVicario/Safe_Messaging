package com.av19.models.api;

public class UpdatePublicKey {
    private String username;
    private String password;
    private String new_public_key;

    public UpdatePublicKey() { }

    public UpdatePublicKey(String username, String password, String new_public_key) {
        this.username = username;
        this.password = password;
        this.new_public_key = new_public_key;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNew_public_key() {
        return new_public_key;
    }

    public void setNew_public_key(String new_public_key) {
        this.new_public_key = new_public_key;
    }
}
