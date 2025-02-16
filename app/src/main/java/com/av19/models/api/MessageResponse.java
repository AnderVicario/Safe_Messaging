package com.av19.models.api;

public class MessageResponse {
    private String sender;
    private String encrypted_message;

    public MessageResponse() { }

    public MessageResponse(String sender, String encrypted_message) {
        this.sender = sender;
        this.encrypted_message = encrypted_message;
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
}
