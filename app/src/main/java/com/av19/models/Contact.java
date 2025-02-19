package com.av19.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Contact {
    private int id;
    private String name;
    private String publicKey; // Nuevo campo
    private List<Message> messages;

    // Constructor actualizado con publicKey
    public Contact(int id, String name, String publicKey, List<Message> messages) {
        this.id = id;
        this.name = name;
        this.publicKey = publicKey;
        this.messages = messages;
    }

    // Getters actualizados
    public int getId() { return id; }
    public String getName() { return name; }
    public String getPublicKey() { return publicKey; } // Nuevo getter
    public List<Message> getMessages() { return messages; }

    public String getLastMessagePreview() {
        if (hasMessages()) {
            Message last = getLastMessage();
            String text = last.getMessage();
            if (text.length() > 30) {
                text = text.substring(0, 30) + "...";
            }
            return (!last.isSender() ? "Tú: " : "") + text;
        }
        return "Sin mensajes";
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