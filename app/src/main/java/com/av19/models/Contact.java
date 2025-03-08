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

        // Usar TimeUnit para mejor conversión
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(diffMillis);
        long days = TimeUnit.MILLISECONDS.toDays(diffMillis);

        if (minutes < 1) return "Ahora";
        if (minutes < 60) return minutes + "m";
        if (hours < 24) return hours + "h";
        if (days < 7) return days + "d";

        // Formato para fechas mayores a 1 semana
        return new SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(messageDate);
    }
}