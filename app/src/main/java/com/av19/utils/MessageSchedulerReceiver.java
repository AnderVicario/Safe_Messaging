package com.av19.utils;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.app.NotificationCompat;

import com.av19.R;
import com.av19.models.api.MessageCreate;
import com.av19.models.api.SendMessageResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageSchedulerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String message = intent.getStringExtra("message");
        String receiver = intent.getStringExtra("receiver");

        // Obtener usuario actual desde SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE);
        String currentUser = prefs.getString("auth_token", null);

        // Enviar mensaje
        MessageCreate messageCreate = new MessageCreate(currentUser, receiver, message);
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.sendMessage(messageCreate).enqueue(new Callback<SendMessageResponse>() {
            @Override
            public void onResponse(Call<SendMessageResponse> call, Response<SendMessageResponse> response) {
                showNotification(context, "Mensaje programado enviado");
            }

            @Override
            public void onFailure(Call<SendMessageResponse> call, Throwable t) {
                showNotification(context, "Error al enviar mensaje programado");
            }
        });
    }

    private void showNotification(Context context, String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "schedule_channel")
                .setSmallIcon(R.drawable.cdnlogo_com_whatsapp2)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
