package com.av19.utils;

import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketClient extends WebSocketListener {
    private static WebSocketClient instance;
    private WebSocket webSocket;
    private OkHttpClient client;
    private OnMessageReceivedListener listener;
    private Context appContext;

    private WebSocketClient(Context context) {
        client = UnsafeOkHttpsClient.getUnsafeOkHttpClient();
        appContext = context.getApplicationContext();
    }

    public static synchronized WebSocketClient getInstance(Context context) {
        if (instance == null) {
            instance = new WebSocketClient(context);
        }
        return instance;
    }

    public void disconnectWebSocket() {
        if (webSocket != null) {
            webSocket.close(1000, "Cierre normal");
            webSocket = null;
        }
    }

    // Definir la interfaz para el callback
    public interface OnMessageReceivedListener {
        void onMessageReceived(String sender);
    }

    // Asignar el listener desde la Activity
    public void setOnMessageReceivedListener(OnMessageReceivedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        System.out.println("WebSocket conectado");
    }

    @Override
    public void onMessage(WebSocket webSocket, String sender) {
        System.out.println("Nuevo mensaje recibido: " + sender);
        if (listener != null) {
            listener.onMessageReceived(sender);
        }
        Intent intent = new Intent("NEW_MESSAGE");
        intent.putExtra("sender", sender);
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(intent);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        System.out.println("Error en WebSocket: " + t.getMessage());
    }

    public void connectWebSocket(String username) {
        if (webSocket != null && isConnected()) {
            System.out.println("WebSocket ya está conectado.");
            return;
        }

        Request request = new Request.Builder()
                .url("wss://umbra.ddns.net:8000/ws/" + username)
                .build();
        webSocket = client.newWebSocket(request, this);
    }

    public boolean isConnected() {
        return webSocket != null;
    }
}
