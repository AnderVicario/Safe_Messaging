package com.av19.utils;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.av19.R;
import com.av19.models.Message;
import com.av19.models.MessageQueue;
import com.av19.models.api.RecieveMessageResponse;
import com.av19.widgets.MessageWidget;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BackgroundWebSocketService extends Service {

    private WebSocketClient webSocketClient;
    private int notificationId = 0;

    // --------------------------------------------------------
    // Ciclo de vida del servicio
    // --------------------------------------------------------

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, createNotification());
        connectWebSocket();
        fetchAndProcessMessages();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webSocketClient != null) {
            webSocketClient.disconnectWebSocket();
        }
        Log.d("BackgroundWebSocketService", "Servicio destruido, WebSocket desconectado");
    }

    // --------------------------------------------------------
    // Conexión y configuración de WebSocket
    // --------------------------------------------------------

    private void connectWebSocket() {
        String user = getCurrentUser();
        if (user == null) return;

        webSocketClient = WebSocketClient.getInstance();
        if (!webSocketClient.isConnected()) {
            webSocketClient.connectWebSocket(user);
        }
        webSocketClient.setOnMessageReceivedListener(new WebSocketClient.OnMessageReceivedListener() {
            @Override
            public void onNewMessageReceived(String sender) {
                fetchAndProcessMessages();
                showNotification(sender);
            }
        });
        fetchAndProcessMessages();
    }

    // --------------------------------------------------------
    // Notificaciones y Foreground Service
    // --------------------------------------------------------

    @SuppressLint("ObsoleteSdkInt")
    private Notification createNotification() {
        String channelId = "background_service";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Servicio de mensajes",
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle(getString(R.string.websocket_service))
                .setSmallIcon(R.drawable.cdnlogo_com_whatsapp2)
                .build();
    }

    @SuppressLint("ObsoleteSdkInt")
    private void showNotification(String sender) {
        String channelId = "messages_channel";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setContentTitle(getString(R.string.websocket_message))
                .setContentText(getString(R.string.websocket_message_from) + " " + sender)
                .setSmallIcon(R.drawable.cdnlogo_com_whatsapp2)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Mensajes",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify(notificationId++, builder.build());
    }

    // --------------------------------------------------------
    // Obtención y procesamiento de mensajes
    // --------------------------------------------------------

    private void fetchAndProcessMessages() {
        String currentUser = getCurrentUser();
        if (currentUser == null) return;

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.getMessages(currentUser).enqueue(new Callback<List<RecieveMessageResponse>>() {
            @Override
            public void onResponse(Call<List<RecieveMessageResponse>> call, Response<List<RecieveMessageResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    processAndStoreMessages(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<RecieveMessageResponse>> call, Throwable t) {
                Log.e(TAG, "Error fetching messages", t);
            }
        });
    }

    private void processAndStoreMessages(List<RecieveMessageResponse> messages) {
        String currentUser = getCurrentUser();
        if (currentUser == null) return;

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this, currentUser);
        SQLiteDatabase db = dbHelper.getEncryptedWritableDatabase();
        Set<Integer> updatedContacts = new HashSet<>();
        MessageQueue messageQueue = MessageQueue.getInstance(this);

        try {
            List<String> existingTimestamps = getExistingTimestamps(db);
            for (RecieveMessageResponse msg : messages) {
                if (shouldSkipMessage(db, msg, existingTimestamps)) continue;

                String sender = msg.getSender().equals(currentUser) ? currentUser : msg.getSender();

                int contactId;
                if (sender.equals(currentUser)){
                    contactId = getContactId(db,  msg.getReceiver());
                }
                else {
                    contactId = getContactId(db, sender);
                }


                if (contactId == -1) continue;

                storeMessage(db, contactId, msg);
                updatedContacts.add(contactId);

                // Widget
                Message messageData = new Message();
                messageData.setContactId(contactId);
                messageData.setMessage(msg.getEncrypted_message());
                messageData.setSender(sender);
                messageData.setSentAt(convertStringToDate(msg.getTimestamp()));
                messageQueue.addMessage(messageData);
            }
            notifyUI(updatedContacts);
        } finally {
            db.close();
        }
        updateWidget();
    }

    // --------------------------------------------------------
    // Procesamiento de mensajes y base de datos
    // --------------------------------------------------------

    private boolean shouldSkipMessage(SQLiteDatabase db, RecieveMessageResponse msg, List<String> existingTimestamps) {
        // Obtener el último mensaje del usuario
        String lastContent = null;
        String lastTimestamp = null;
        try (Cursor cursor = db.rawQuery(
                "SELECT message FROM messages WHERE is_sender = 1 ORDER BY sent_at DESC LIMIT 1",
                null
        )) {
            if (cursor.moveToFirst()) {
                lastContent = cursor.getString(0);
            }
        }

        // Calcular si el nuevo mensaje es igual al último
        boolean isDuplicateRecentMessage = false;
        if (lastContent != null) {
            if (lastContent.equals(msg.getEncrypted_message())) {
                isDuplicateRecentMessage = true;
            }
        }

        // Condición final
        return existingTimestamps.contains(msg.getTimestamp())
                || msg.getIs_initial()
                || isDuplicateRecentMessage;
    }

    private List<String> getExistingTimestamps(SQLiteDatabase db) {
        List<String> timestamps = new ArrayList<>();
        try (Cursor cursor = db.rawQuery("SELECT sent_at FROM messages", null)) {
            while (cursor.moveToNext()) {
                timestamps.add(cursor.getString(0));
            }
        }
        return timestamps;
    }

    private int getContactId(SQLiteDatabase db, String contactName) {
        try (Cursor cursor = db.rawQuery("SELECT id FROM contacts WHERE name = ?", new String[]{contactName})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : -1;
        }
    }

    private void storeMessage(SQLiteDatabase db, int contactId, RecieveMessageResponse msg) {
        ContentValues values = new ContentValues();
        values.put("contact_id", contactId);
        values.put("is_sender", msg.getSender().equals(getCurrentUser()) ? 1 : 0);
        values.put("message", msg.getEncrypted_message());
        values.put("sent_at", msg.getTimestamp());
        db.insert("messages", null, values);
    }

    private void notifyUI(Set<Integer> updatedContacts) {
        Intent intent = new Intent("NEW_MESSAGES_ADDED");
        intent.putIntegerArrayListExtra("updated_contacts", new ArrayList<>(updatedContacts));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // ---------
    // Otros
    // ---------

    private String getCurrentUser() {
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        return prefs.getString("auth_token", null);
    }

    private Date convertStringToDate(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return sdf.parse(dateString);
        } catch (ParseException e) {
            Log.e("ContactList", "Error parsing date: " + dateString, e);
            return null;
        }
    }

    private void updateWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName widgetComponent = new ComponentName(this, MessageWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);
        new MessageWidget().onUpdate(this, appWidgetManager, appWidgetIds);
    }
}
