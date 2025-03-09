package com.av19.models;

import android.content.Context;

import com.av19.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Contact {
    private int id;
    private String name;
    private byte[] photo;
    private List<Message> messages;
    private Context context;

    public Contact(int id, String name, byte[] photo, List<Message> messages, Context context) {
        this.id = id;
        this.name = name;
        this.photo = photo;
        this.messages = messages;
        this.context = context;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public byte[] getPhoto() { return photo; }
    public List<Message> getMessages() { return messages; }

    public String getLastMessagePreview() {
        if (hasMessages()) {
            Message last = getLastMessage();
            String text = last.getMessage();
            if (text.length() > 30) {
                text = text.substring(0, 30) + "...";
            }
            return (last.getIsSender() ? context.getString(R.string.you) + ": " : "") + text;
        }
        return context.getString(R.string.no_messages);
    }

    public String getFormattedLastMessageTime() {
        return hasMessages() ? formatTimeDifference(getLastMessage().getSentAt()) : "";
    }

    public Date getLastMessageDate() {
        return hasMessages() ? getLastMessage().getSentAt() : null;
    }

    private boolean hasMessages() {
        return messages != null && !messages.isEmpty();
    }

    private Message getLastMessage() {
        return messages.get(messages.size() - 1);
    }

    private String formatTimeDifference(Date messageDate) {
        long diffMillis = System.currentTimeMillis() - messageDate.getTime();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(diffMillis);
        long days = TimeUnit.MILLISECONDS.toDays(diffMillis);

        if (minutes < 1) return context.getString(R.string.just_now);
        if (minutes < 60) return minutes + context.getString(R.string.minute);
        if (hours < 24) return hours + context.getString(R.string.hour);
        if (days < 7) return days + context.getString(R.string.day);

        // Formato para fechas mayores a 1 semana
        return new SimpleDateFormat(context.getString(R.string.day_format), Locale.getDefault()).format(messageDate);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }
}