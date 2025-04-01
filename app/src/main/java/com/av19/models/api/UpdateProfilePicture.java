package com.av19.models.api;

public class UpdateProfilePicture {
    private String username;
    private String password;
    private byte[] profile_picture;

    public UpdateProfilePicture() { }

    public UpdateProfilePicture(String username, String password, byte[] profile_picture) {
        this.username = username;
        this.password = password;
        this.profile_picture = profile_picture;
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

    public byte[] getProfile_picture() {
        return profile_picture;
    }

    public void setProfile_picture(byte[] profile_picture) {
        this.profile_picture = profile_picture;
    }
}
