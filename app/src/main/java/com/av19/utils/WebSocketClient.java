package com.av19.utils;

import org.json.JSONException;
import org.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketClient extends WebSocketListener {
    private static WebSocketClient instance;
    private WebSocket webSocket;
    private final OkHttpClient client;
    private OnMessageReceivedListener listener;

    private WebSocketClient() {
        client = UnsafeOkHttpsClient.getUnsafeOkHttpClient();
    }

    public static synchronized WebSocketClient getInstance() {
        if (instance == null) {
            instance = new WebSocketClient();
        }
        return instance;
    }

    // Para notificar cuando se reciba un nuevo mensaje
    public interface OnMessageReceivedListener {
        void onNewMessageReceived(String sender);
    }

    public void setOnMessageReceivedListener(OnMessageReceivedListener listener) {
        this.listener = listener;
    }

    // Conexión al servidor
    public void connectWebSocket(String username) {
        if (isConnected()) {
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

    // Cerrar la conexión
    public void disconnectWebSocket() {
        if (isConnected()) {
            webSocket.close(1000, "Cierre normal");
            webSocket = null;
        }
    }

    // Se llama cuando la conexión se establece
    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        System.out.println("WebSocket conectado");
    }

    // Se llama al recibir un mensaje
    @Override
    public void onMessage(WebSocket webSocket, String jsonMessage) {
        try {
            JSONObject message = new JSONObject(jsonMessage);
            if ("new_message".equals(message.getString("type"))) {
                String sender = message.getString("sender");
                if (listener != null) {
                    listener.onNewMessageReceived(sender);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Para manejar errores
    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        System.out.println("Error en WebSocket: " + t.getMessage());
    }
}
