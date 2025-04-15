package com.av19.models;
import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MessageQueue {
    private static final String PREFS_NAME = "session";
    private static final String KEY_QUEUE = "message_queue";
    private static final int MAX_MESSAGES = 5;

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public MessageQueue(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void addMessage(Message message) {
        List<Message> messages = getAllMessages();
        messages.add(0, message);

        // Mantener solo los últimos 5 mensajes
        if(messages.size() > MAX_MESSAGES) {
            messages = messages.subList(0, MAX_MESSAGES);
        }

        saveMessages(messages);
    }

    public List<Message> getAllMessages() {
        String json = prefs.getString(KEY_QUEUE, "[]");
        Type type = new TypeToken<ArrayList<Message>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public void clear() {
        prefs.edit().putString(KEY_QUEUE, "[]").apply();
    }

    private void saveMessages(List<Message> messages) {
        prefs.edit()
                .putString(KEY_QUEUE, gson.toJson(messages))
                .apply();
    }
}
