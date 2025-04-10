package com.av19.models.api;

public class SendMessageResponse {
    public String message;
    public String timestamp;
    public String is_initial;

    public SendMessageResponse(String message, String timestamp, String is_initial) {
        this.message = message;
        this.timestamp = timestamp;
        this.is_initial = is_initial;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String isIs_initial() {
        return is_initial;
    }

    public void setIs_initial(String is_initial) {
        this.is_initial = is_initial;
    }
}
