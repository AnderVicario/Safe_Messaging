package com.av19;

import java.util.Date;
import java.util.List;

public class Contact {
    private int id;
    private String name;
    private List<Message> messages;

    public Contact(int id, String name, List<Message> messages) {
        this.id = id;
        this.name = name;
        this.messages = messages;
    }

    public String getName() {
        return name;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public String getLastMessage() {
        if (messages != null && !messages.isEmpty()) {
            return messages.get(messages.size() - 1).getMessage(); // Último mensaje
        }
        return null; // Retorna null si no hay mensajes
    }

    public String getLastMessageTime() {
        if (messages != null && !messages.isEmpty()) {
            Date lastSentAt = messages.get(messages.size() - 1).getSentAt(); // Fecha del último mensaje
            return formatTimeDifference(lastSentAt);
        }
        return null; // Retorna null si no hay mensajes
    }

    public Date getLastMessageDate() {
        if (messages != null && !messages.isEmpty()) {
            return messages.get(messages.size() - 1).getSentAt(); // Devuelve la fecha del último mensaje
        }
        return null; // Retorna null si no hay mensajes
    }

    private String formatTimeDifference(Date date) {
        long timeDifference = System.currentTimeMillis() - date.getTime(); // Diferencia de tiempo en milisegundos
        long seconds = timeDifference / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long years = days / 365;

        // Determina la unidad más apropiada
        if (seconds < 60) {
            return seconds + "s"; // segundos
        } else if (minutes < 60) {
            return minutes + "m"; // minutos
        } else if (hours < 24) {
            return hours + "h"; // horas
        } else if (days < 365) {
            return days + "d"; // días
        } else {
            return years + "y"; // años
        }
    }
}
