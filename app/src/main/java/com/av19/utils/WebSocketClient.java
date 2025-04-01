package com.av19.utils;

import okhttp3.*;

public class WebSocketClient extends WebSocketListener {
    private WebSocket webSocket;
    private OnMessageReceivedListener listener;

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
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        System.out.println("Error en WebSocket: " + t.getMessage());
    }

    public void connectWebSocket(String username) {
        OkHttpClient client = UnsafeOkHttpsClient.getUnsafeOkHttpClient();
        Request request = new Request.Builder().url("wss://umbra.ddns.net:8000/ws/" + username).build();
        webSocket = client.newWebSocket(request, this);
    }
}
