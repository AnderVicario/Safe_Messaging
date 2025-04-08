package com.av19.models.api;

public class RecieveMessageResponse {
    private String sender;
    private String encrypted_message;
    private String timestamp;
    private Boolean is_initial;

    public RecieveMessageResponse() { }

    public RecieveMessageResponse(String sender, String encrypted_message, String timestamp, Boolean is_initial) {
        this.sender = sender;
        this.encrypted_message = encrypted_message;
        this.timestamp = timestamp;
        this.is_initial = is_initial;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getEncrypted_message() {
        return encrypted_message;
    }

    public void setEncrypted_message(String encrypted_message) {
        this.encrypted_message = encrypted_message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getIs_initial() {
        return is_initial;
    }

    public void setIs_initial(Boolean is_initial) {
        this.is_initial = is_initial;
    }
}
