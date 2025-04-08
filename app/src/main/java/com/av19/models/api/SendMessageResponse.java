package com.av19.models.api;
import java.util.Date;

public class SendMessageResponse {
    public String message;
    public Date timestamp;
    public boolean is_initial;

    public SendMessageResponse(String message, Date timestamp, boolean is_initial) {
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

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isIs_initial() {
        return is_initial;
    }

    public void setIs_initial(boolean is_initial) {
        this.is_initial = is_initial;
    }
}
