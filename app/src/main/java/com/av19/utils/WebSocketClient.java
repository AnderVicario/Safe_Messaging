package com.av19.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketClient extends WebSocketListener {
    private static final String TAG = "WebSocketClient";
    private static WebSocketClient instance;
    private WebSocket webSocket;
    private boolean isOpen = false;
    private boolean manualClose = false;
    private String lastUsername;
    private final OkHttpClient client;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final long RECONNECT_DELAY_MS = 5_000;
    private OnMessageReceivedListener listener;

    private WebSocketClient() {
        client = UnsafeOkHttpsClient.getUnsafeOkHttpClient()
                .newBuilder()
                .pingInterval(30, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized WebSocketClient getInstance() {
        if (instance == null) {
            instance = new WebSocketClient();
        }
        return instance;
    }

    public interface OnMessageReceivedListener {
        void onNewMessageReceived(String sender);
    }

    public void setOnMessageReceivedListener(OnMessageReceivedListener listener) {
        this.listener = listener;
    }

    public void connectWebSocket(String username) {
        if (isOpen && webSocket != null) {
            Log.i(TAG, "WebSocket ya está conectado.");
            return;
        }
        manualClose = false;
        lastUsername = username;
        Request request = new Request.Builder()
                .url("wss://umbra.ddns.net:8000/ws/" + username)
                .build();
        webSocket = client.newWebSocket(request, this);
    }

    public boolean isConnected() {
        return isOpen && webSocket != null;
    }

    public void disconnectWebSocket() {
        manualClose = true;
        if (isOpen && webSocket != null) {
            webSocket.close(1000, "Cierre normal");
        }
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        this.webSocket = webSocket;
        isOpen = true;
        Log.i(TAG, "Conexión WebSocket abierta");
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        isOpen = false;
        Log.i(TAG, "Conexión WebSocket cerrándose: " + reason);
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        isOpen = false;
        this.webSocket = null;
        Log.i(TAG, "Conexión WebSocket cerrada: " + reason);
        attemptReconnect();
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        isOpen = false;
        this.webSocket = null;
        Log.e(TAG, "Error en WebSocket: " + t.getMessage(), t);
        attemptReconnect();
    }

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
            Log.e(TAG, "Error al parsear mensaje JSON", e);
        }
    }

    private void attemptReconnect() {
        if (manualClose) {
            Log.i(TAG, "No reconecto porque fue cierre manual");
            return;
        }
        Log.i(TAG, "Reintentando conexión en " + (RECONNECT_DELAY_MS / 1000) + "s...");
        handler.postDelayed(() -> {
            Log.i(TAG, "Reconectando...");
            connectWebSocket(lastUsername);
        }, RECONNECT_DELAY_MS);
    }
}
