package com.av19;

import java.util.Date;

public class Message {
    private String message;
    private Date sentAt; // Cambiado a Date

    public Message(String message, Date sentAt) {
        this.message = message;
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
}
