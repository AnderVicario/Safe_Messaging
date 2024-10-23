package com.av19;

public class Contact {
    private String name;
    private String lastMessage;
    private String lastMessageTime;

    public Contact(String name, String lastMessage, String lastMessageTime) {
        this.name = name;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(String lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public int getLastMessageTimeInMinutes() {
        if (lastMessageTime.endsWith("m")) {
            return Integer.parseInt(lastMessageTime.replace("m", ""));
        } else if (lastMessageTime.endsWith("h")) {
            return Integer.parseInt(lastMessageTime.replace("h", "")) * 60;
        } else if (lastMessageTime.endsWith("d")) {
            return Integer.parseInt(lastMessageTime.replace("d", "")) * 60 * 24;
        } else if (lastMessageTime.endsWith("y")) {
            return Integer.parseInt(lastMessageTime.replace("y", "")) * 60 * 24 * 365;
        }
        return 0;
    }
}
