package com.av19.models.api;

public class MessageCreate {
    private String sender;
    private String receiver;
    private String encrypted_message;

    public MessageCreate() { }

    public MessageCreate(String sender, String receiver, String encrypted_message) {
        this.sender = sender;
        this.receiver = receiver;
        this.encrypted_message = encrypted_message;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getEncrypted_message() {
        return encrypted_message;
    }

    public void setEncrypted_message(String encrypted_message) {
        this.encrypted_message = encrypted_message;
    }
}
