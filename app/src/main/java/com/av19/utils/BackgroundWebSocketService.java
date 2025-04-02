package com.av19.utils;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.av19.R;

public class BackgroundWebSocketService extends Service {
    private WebSocketClient webSocketClient;
    private int notificationId = 0;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, createNotification());
        connectWebSocket();
        return START_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

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

    private void connectWebSocket() {
        String user = getCurrentUser();
        if (user == null) return;

        webSocketClient = WebSocketClient.getInstance(this);
        if (!webSocketClient.isConnected()) {
            webSocketClient.connectWebSocket(user);
        }
        webSocketClient.setOnMessageReceivedListener(this::showNotification);
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

    private String getCurrentUser() {
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        return prefs.getString("auth_token", null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webSocketClient != null) {
            webSocketClient.disconnectWebSocket();
        }
        Log.d("BackgroundWebSocketService", "Servicio destruido, WebSocket desconectado");
    }
}
