package com.av19.models;

import java.util.Date;

public class Message {
    private String message;
    private boolean isSender;
    private Date sentAt;

    public Message(String message, boolean pIsSender, Date sentAt) {
        this.message = message;
        this.isSender = pIsSender;
        this.sentAt = sentAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getSentAt() {
        return sentAt;
    }

    public void setSentAt(Date sentAt) {
        this.sentAt = sentAt;
    }

    public boolean isSender() {
        return isSender;
    }

    public void setSender(boolean sender) {
        isSender = sender;
    }
}
