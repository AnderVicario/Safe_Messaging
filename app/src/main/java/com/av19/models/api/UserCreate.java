package com.av19.models.api;

public class UserCreate {
    private String username;
    private String password;
    private String public_key;

    public UserCreate() { }

    public UserCreate(String username, String password, String public_key) {
        this.username = username;
        this.password = password;
        this.public_key = public_key;
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

    public String getPublic_key() {
        return public_key;
    }

    public void setPublic_key(String public_key) {
        this.public_key = public_key;
    }
}
