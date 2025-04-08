package com.av19.models.api;

public class ProfilePictureResponse {
    private String profile_picture;

    public ProfilePictureResponse() { }

    public ProfilePictureResponse(String profile_picture) {
        this.profile_picture = profile_picture;
    }

    public String getProfile_picture() {
        return profile_picture;
    }

    public void setProfile_picture(String profile_picture) {
        this.profile_picture = profile_picture;
    }
}
