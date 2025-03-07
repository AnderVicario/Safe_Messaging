package com.av19.models;

import java.util.Date;

public class Message {
    private Integer id;
    private Integer contactId;
    private String message;
    private boolean isSender;
    private Date sentAt;

    public Message(Integer id, Integer contactId, String message, boolean isSender, Date sentAt) {
        this.id = id;
        this.contactId = contactId;
        this.message = message;
        this.isSender = isSender;
        this.sentAt = sentAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getContactId() {
        return contactId;
    }

    public void setContactId(Integer contactId) {
        this.contactId = contactId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean getIsSender() {
        return isSender;
    }

    public void setSender(boolean sender) {
        isSender = sender;
    }

    public Date getSentAt() {
        return sentAt;
    }

    public void setSentAt(Date sentAt) {
        this.sentAt = sentAt;
    }
}
